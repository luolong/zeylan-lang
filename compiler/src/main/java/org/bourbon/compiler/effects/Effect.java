package org.bourbon.compiler.effects;

import java.lang.annotation.ElementType;

import java.lang.annotation.Target;

/// An informative annotation type used to indicate that an interface type declaration is intended to be used as an effect handler.
///
/// Effect handlers are supposed to be registered in effect context before executing functionality that uses the effect
@Target({ ElementType.TYPE })
public @interface Effect {

    /// An informative annotation type used to indicate that an interface type declaration is intended to be used as an effect handler.
    @interface Handler {

    }
}
