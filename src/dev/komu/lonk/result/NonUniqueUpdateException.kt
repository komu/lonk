package dev.komu.lonk.result

/**
 * Exception thrown when expecting update to modify a single row, but zero or multiple rows were modified.
 * 
 * @see Database.updateUnique
 */
public class NonUniqueUpdateException(count: Int) :
    UnexpectedResultException("Expected single row to be updated, but database updated $count rows")
