package com.dreamfirestudios.dreamconfig.Interface;

/**
 * <summary>Generic contract for validating a config instance.</summary>
 * <typeparam name="T">Config type to validate.</typeparam>
 */
public interface ConfigValidator<T> {
    /**
     * <summary>Validate the given config instance.</summary>
     * <param name="config">Instance to validate.</param>
     * <returns>
     *  A human-readable error message when validation fails; otherwise {@code null} to indicate success.
     * </returns>
     */
    String validate(T config);
}