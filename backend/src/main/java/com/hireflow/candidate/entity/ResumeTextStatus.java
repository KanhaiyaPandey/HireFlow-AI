package com.hireflow.candidate.entity;

/** Result of extracting text from an uploaded PDF. */
public enum ResumeTextStatus {
    /** Text was found and stored. */
    EXTRACTED,
    /** The PDF has no text layer (e.g. a scanned image), so AI screening is unavailable for it. */
    EMPTY,
    /** The PDF could not be read. */
    FAILED
}
