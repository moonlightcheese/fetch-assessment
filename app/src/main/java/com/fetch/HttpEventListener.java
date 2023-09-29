package com.fetch;

public interface HttpEventListener {
    /**
     * This callback occurs before flush() and close() of stream just so we know that the transfer completed.
     * If there are problems with closing the stream or flushing the buffer, then this callback will occur but
     * the onFileReceived callback will not and therefore we get a bit more information about the process.
     */
    void onResponseReceived(String response);

    /**
     * This is called in the case of an error or Exception.
     * @param message
     * @param e
     */
    void onError(String message, Exception e);
}
