package dev.komu.lonk.result

/**
 * Exception thrown when expecting a unique result from a call, but got no results.
 * 
 * @see Database.findUnique
 * @see NonUniqueResultException
 */
public class EmptyResultException : NonUniqueResultException("Expected unique result, but got no rows")
