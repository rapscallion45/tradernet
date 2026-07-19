package com.tradernet.api.resources;

/**
 * Standard JSON error payload for HTTP-level API failures.
 */
public class ApiErrorDto {

    private ErrorBody error;

    public ApiErrorDto() {
    }

    public ApiErrorDto(int status, String code, String errorMessage) {
        this.error = new ErrorBody(status, code, errorMessage, System.currentTimeMillis());
    }

    public ErrorBody getError() {
        return error;
    }

    public void setError(ErrorBody error) {
        this.error = error;
    }

    public static class ErrorBody {
        private int status;
        private String code;
        private String errorMessage;
        private long timestamp;

        public ErrorBody() {
        }

        public ErrorBody(int status, String code, String errorMessage, long timestamp) {
            this.status = status;
            this.code = code;
            this.errorMessage = errorMessage;
            this.timestamp = timestamp;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
