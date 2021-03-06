package com.nhl.link.rest.meta;

import org.apache.cayenne.exp.parser.ASTPath;

/**
 * Represents an entity "simple" property.
 * 
 * @since 1.12
 */
public interface LrAttribute {

	String getName();

	/**
	 * @since 1.24
	 */
	Class<?> getType();

	ASTPath getPathExp();
}
