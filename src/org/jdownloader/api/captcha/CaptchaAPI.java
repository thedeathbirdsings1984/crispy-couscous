package org.jdownloader.api.captcha;

import java.util.List;

import jd.controlling.captcha.SkipRequest;

import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPIInterface;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.remoteapi.annotations.APIParameterNames;
import org.appwork.remoteapi.annotations.ApiDoc;
import org.appwork.remoteapi.annotations.ApiNamespace;
import org.appwork.remoteapi.exceptions.APIError;
import org.appwork.remoteapi.exceptions.InternalApiException;

@ApiNamespace("captcha")
public interface CaptchaAPI extends RemoteAPIInterface {
    public static enum ABORT {
        SINGLE,
        HOSTER,
        ALL
    }

    public static enum Error implements APIError {
        NOT_AVAILABLE(ResponseCode.ERROR_NOT_FOUND),
        UNKNOWN_CHALLENGETYPE(ResponseCode.ERROR_NOT_FOUND);
        private Error(ResponseCode code) {
            this.code = code;
        }

        private ResponseCode code;

        @Override
        public ResponseCode getCode() {
            return code;
        }
    }

    @ApiDoc("returns a list of all available captcha jobs")
    @APIParameterNames({ "request" })
    public List<CaptchaJob> list(RemoteAPIRequest request);

    @ApiDoc("Returns Captcha Image as Base64 encoded data url")
    @APIParameterNames({ "request", "response", "id" })
    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id) throws InternalApiException, InvalidCaptchaIDException;

    @ApiDoc("Returns Captcha Image as Base64 encoded data url")
    @APIParameterNames({ "request", "response", "id", "format" })
    public void get(RemoteAPIRequest request, final RemoteAPIResponse response, final long id, String format) throws InternalApiException, InvalidCaptchaIDException;

    @ApiDoc("Returns CaptchaJob Object for the given id")
    @APIParameterNames({ "request", "id" })
    public CaptchaJob getCaptchaJob(RemoteAPIRequest request, final long id) throws InvalidCaptchaIDException;

    @APIParameterNames({ "id" })
    public long keepAlive(final long id);

    @APIParameterNames({ "id", "result" })
    public boolean solve(final long id, String result) throws InvalidCaptchaIDException, InvalidChallengeTypeException;

    @APIParameterNames({ "id", "result", "resultFormat" })
    public boolean solve(final long id, String result, String resultFormat) throws InvalidCaptchaIDException, InvalidChallengeTypeException;

    @Deprecated
    /**
     * @deprecated use #skip(id,type) instead
     * @param id
     * @return
     * @throws InvalidCaptchaIDException
     */
    @APIParameterNames({ "id" })
    public boolean skip(final long id) throws InvalidCaptchaIDException;

    @APIParameterNames({ "id", "type" })
    public boolean skip(final long id, SkipRequest type) throws InvalidCaptchaIDException;
}
