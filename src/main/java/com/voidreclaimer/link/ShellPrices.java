package com.voidreclaimer.link;

/**
 * How dear Remote Shells are, a config choice ({@code link.shellPrices}). Each has its own recipes, picked by
 * {@link com.voidreclaimer.machine.ConfigChoiceCondition}. A shell holds a whole basic body, and scrapping one gives its parts back,
 * so anything under {@link #PARTS} is a cheaper way to buy those parts.
 */
public enum ShellPrices {
    /** The first prices: well under the parts inside. */
    CHEAP,
    /** Three quarters of the parts inside. */
    REDUCED,
    /** What the parts inside cost. */
    PARTS,
    /** A quarter over the parts inside. */
    PREMIUM
}
