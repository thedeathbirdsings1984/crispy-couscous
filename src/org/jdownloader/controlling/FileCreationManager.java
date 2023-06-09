package org.jdownloader.controlling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import jd.controlling.downloadcontroller.BadDestinationException;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.PathTooLongException;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;
import org.jdownloader.translate._JDT;
import org.jdownloader.utils.JDFileUtils;

public class FileCreationManager {
    public static enum DeleteOption implements LabelInterface {
        NO_DELETE {
            @Override
            public String getLabel() {
                return _JDT.T.DeleteOption_no_delete();
            }
        },
        RECYCLE {
            @Override
            public String getLabel() {
                return _JDT.T.DeleteOption_recycle();
            }
        },
        NULL {
            @Override
            public String getLabel() {
                return _JDT.T.DeleteOption_final_delete();
            }
        };
    }

    private static final FileCreationManager INSTANCE = new FileCreationManager();

    /**
     * get the only existing instance of FileCreationManager. This is a singleton
     *
     * @return
     */
    public static FileCreationManager getInstance() {
        return FileCreationManager.INSTANCE;
    }

    private FileCreationEventSender eventSender;
    private LogSource               logger;

    public FileCreationEventSender getEventSender() {
        return eventSender;
    }

    /**
     * Create a new instance of FileCreationManager. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private FileCreationManager() {
        eventSender = new FileCreationEventSender();
        logger = LogController.getInstance().getLogger(FileCreationManager.class.getName());
    }

    public boolean mkdir(File folder) {
        if (folder.exists()) {
            return false;
        }
        List<File> backlog = new ArrayList<File>();
        HashSet<String> loopcheck = new HashSet<String>();
        File copy = folder;
        while (copy != null && !copy.exists()) {
            if (loopcheck.add(copy.getAbsolutePath())) {
                backlog.add(copy);
            }
            copy = copy.getParentFile();
        }
        for (int i = backlog.size() - 1; i >= 0; i--) {
            if (mkdirInternal(backlog.get(i))) {
                getEventSender().fireEvent(new FileCreationEvent(this, FileCreationEvent.Type.NEW_FOLDER, backlog.get(i)));
            } else {
                return false;
            }
        }
        return true;
    }

    private boolean mkdirInternal(File file) {
        try {
            DownloadWatchDog.getInstance().validateDestination(file);
            return file.mkdir();
        } catch (PathTooLongException e) {
        } catch (BadDestinationException e) {
        }
        return false;
    }

    /**
     * undefined handling of DeleteOption.RECYCLE without trash support(eg linux),see JDFileUtils.isTrashSupported()
     *
     * @param file
     * @param deleteTo
     * @return
     */
    @Deprecated
    public boolean delete(File file, DeleteOption deleteTo) {
        if (deleteTo == null) {
            deleteTo = FileCreationManager.DeleteOption.NULL;
        }
        if (!file.exists()) {
            return true;
        } else {
            switch (deleteTo) {
            case NULL:
                if (file.delete()) {
                    return true;
                } else {
                    if (Application.getJavaVersion() >= Application.JAVA17) {
                        try {
                            java.nio.file.Files.delete(file.toPath());
                        } catch (Exception e) {
                            logger.log(e);
                        }
                    }
                    return !file.exists();
                }
            case RECYCLE:
                if (JDFileUtils.isTrashSupported()) {
                    try {
                        JDFileUtils.moveToTrash(file);
                    } catch (IOException e) {
                        logger.log(e);
                    }
                }
                return !file.exists();
            case NO_DELETE:
            default:
                return false;
            }
        }
    }

    public void moveFile(String oldPath, String newPath) {
        if (new File(oldPath).exists() && !new File(newPath).exists()) {
            FileCreationManager.getInstance().mkdir(new File(newPath).getParentFile());
            if (!new File(oldPath).renameTo(new File(newPath))) {
                try {
                    IO.copyFile(new File(oldPath), new File(newPath));
                    FileCreationManager.getInstance().delete(new File(oldPath), null);
                } catch (IOException e) {
                    logger.log(e);
                }
            }
        }
    }
}
