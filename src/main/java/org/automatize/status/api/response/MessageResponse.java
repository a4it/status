package org.automatize.status.api.response;

/**
 * Generic response object for simple API operations.
 * <p>
 * Used to return a success/failure status along with a descriptive message
 * for operations that don't require complex response data.
 * </p>
 */
public class MessageResponse {

    /** The response message. */
    private String message;

    /** Whether the operation was successful. */
    private boolean success;

    /**
     * Default constructor.
     */
    public MessageResponse() {
    }

    /**
     * Constructs a MessageResponse with the specified message and success status.
     *
     * @param message the response message
     * @param success true if the operation was successful, false otherwise
     */
    public MessageResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     *
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the success status.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Sets the success status.
     *
     * @param success the success status to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
}