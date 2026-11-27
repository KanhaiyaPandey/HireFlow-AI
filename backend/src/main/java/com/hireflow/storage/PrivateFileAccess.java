package com.hireflow.storage;

import java.net.URI;

/**
 * How to hand a private file to someone who is allowed to see it. Sealed, so a controller's
 * {@code switch} must handle both cases.
 */
public sealed interface PrivateFileAccess {

    /** Send the client to a short-lived signed URL (Cloudinary). The file never passes through the API. */
    record Redirect(URI url) implements PrivateFileAccess {
    }

    /** Stream the bytes from the API (local disk). */
    record Content(byte[] bytes, String contentType, String downloadName) implements PrivateFileAccess {
    }
}
