/* 
 * Copyright (C) 2013 The Java Tool project
 * Gelin Luo <greenlaw110(at)gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.osgl;

import org.osgl.exception.FastRuntimeException;
import org.osgl.exception.NotAppliedException;
import org.osgl.exception.UnexpectedException;
import org.osgl.util.*;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * <code>_</code> is the umbrella namespace aggregates core utilities of OSGL toolkit:
 * <p/>
 * <ul>
 * <li>Function interfaces and base implementations</li>
 * <li>currying utilities </li>
 * <li>Tuple and multi-elements tupbles</li>
 * <li>Option</li>
 * <li>core utilities like ts()</li>
 * <li>predefined functions aggregated in the <code>_.F</code> namespace</li>
 * </ul>
 * <p/>
 * <p>More about function interface</p>
 * <p/>
 * <p>Under <code>_</code>, there are six function interfaces defined, from <code>_.Func0</code>
 * to <code>_.Func5</code>, where the last digit means the number of parameters the function
 * is applied to. For example, the <code>apply</code> method of <code>Func0</code> takes
 * no parameter while that of <code>Func2</code> takes two parameters. All these function
 * interfaces are defined with generic type parameters, corresponding to the type of all
 * parameters and that of the return value. For procedure (a function that does not return anything),
 * the user application could use <code>Void</code> as the return value type, and return
 * <code>null</code> in the <code>apply</code> method implementation.</p>
 * <p/>
 * <p>For each function interface, OSGL provide a base class, from <code>_.F0</code> to
 * <code>_.F5</code>. Within the base class, OSGL implement several utility methods, including</p>
 * <p/>
 * <ul>
 * <li>currying methods, returns function takes fewer parameter with given parameter specified</li>
 * <li>chain, returns composed function with a function takes the result of this function</li>
 * <li>andThen, returns composed function with an array of same signature functions</li>
 * <li>breakOut, short cut the function execution sequence by throwing out a {@link Break} instance</li>
 * </ul>
 * <p/>
 * <p>Usually user application should define their function implementation by extending the base class in order
 * to benefit from the utility methods; however in certain cases, e.g. a function implementation already
 * extends another base class, user implementation cannot extends the base function class, OSGL provides
 * easy way to convert user's implementation to corresponding base class implementation, here is one
 * example of how to do it:</p>
 * <p/>
 * <pre>
 *     void foo(_.Func2<Integer, String> f) {
 *         F2<Integer, String> newF = _.f2(f);
 *         newF.chain(...);
 *         ...
 *     }
 * </pre>
 * <p/>
 * <p>Dumb functions, for certain case where a dumb function is needed, OSGL defines dumb function instances for
 * each function interface, say, from <code>_.F0</code> to <code>_.F5</code>. Note the name of the dumb function
 * instance is the same as the name of the base function class. But they belong to different concept, class and
 * instance, so there is no conflict in the code. For each dumb function instance, a corresponding type safe
 * version is provided, <code>_.f0()</code> to <code>_.f5()</code>, this is the same case of
 * <code>java.util.Collections.EMPTY_LIST</code> and <code>java.util.Collections.emptyList()</code></p>
 * <p/>
 * <p>Utility methods</p>
 *
 * @author Gelin Luo
 * @version 0.2
 */
public enum _ {
    INSTANCE;

    // --- Functions and their default implementations

    /**
     * Define a function that apply to no parameter (strictly this is not a function)
     *
     * @param <R> the generic type of the return value, could be <code>Void</code>
     * @see org.osgl._.Function
     * @see org.osgl._.Func2
     * @see org.osgl._.Func3
     * @see org.osgl._.Func4
     * @see org.osgl._.Func5
     * @see F0
     * @since 0.2
     */
    public static interface Func0<R> {

        /**
         * user application to implement main logic of applying the function
         *
         * @throws NotAppliedException if the function doesn't apply to the current context
         * @throws Break               to short cut collecting operations (fold/reduce) on an {@link org.osgl.util.C.Traversable container}
         */
        R apply() throws NotAppliedException, Break;

    }

    /**
     * Default implementation for {@link org.osgl._.Func0}. Implementation of {@link org.osgl._.Func0} should
     * (nearly) always extend to this class instead of implement the interface directly
     *
     * @since 0.2
     */
    public static abstract class F0<R> implements Func0<R> {

        /**
         * Return a {@link Break} with payload. Here is an example of how to use this method:
         * <p/>
         * <pre>
         *     myData.accept(new Visitor(){
         *         public void apply(T e) {
         *             if (...) {
         *                 throw breakOut(e);
         *             }
         *         }
         *     })
         * </pre>
         *
         * @param payload the object passed through the <code>Break</code>
         * @return a {@link Break} instance
         */
        protected Break breakOut(Object payload) {
            return new Break(payload);
        }

        /**
         * Applies this partial function to the given argument when it is contained in the function domain.
         * Applies fallback function where this partial function is not defined.
         *
         * @param fallback
         * @return the result of this function or the fallback function application
         */
        public R applyOrElse(F0<? extends R> fallback) {
            try {
                return apply();
            } catch (NotAppliedException e) {
                return fallback.apply();
            }
        }

        /**
         * Returns a composed function that applies this function to it's input and
         * then applies the {@code after} function to the result. If evaluation of either
         * function throws an exception, it is relayed to the caller of the composed
         * function.
         *
         * @param after the function applies after this function is applied
         * @param <T>   the type of the output of the {@code before} function
         * @return the composed function
         * @throws NullPointerException if {@code before} is null
         */
        public <T> F0<T> andThen(final Function<? super R, ? extends T> after) {
            E.NPE(after);
            final F0<R> me = this;
            return new F0<T>() {
                @Override
                public T apply() {
                    return after.apply(me.apply());
                }
            };
        }

        /**
         * Returns a composed function that applied, in sequence, this function and
         * all functions specified one by one. If applying anyone of the functions
         * throws an exception, it is relayed to the caller of the composed function.
         * If an exception is thrown out, the following functions will not be applied.
         * <p/>
         * <p>When apply the composed function, the result of the last function
         * is returned</p>
         *
         * @param fs a sequence of function to be applied after this function
         * @return a composed function
         */
        public F0<R> andThen(final Func0<? extends R>... fs) {
            if (fs.length == 0) {
                return this;
            }
            final F0<R> me = this;
            return new F0<R>() {
                @Override
                public R apply() {
                    R r = me.apply();
                    for (Func0<? extends R> f : fs) {
                        r = f.apply();
                    }
                    return r;
                }
            };
        }

        /**
         * Returns a composed function that when applied, try to apply this function first, in case
         * a {@link NotAppliedException} is captured apply to the fallback function specified. This
         * method helps to implement partial function
         *
         * @param fallback the function to applied if this function doesn't apply in the current situation
         * @return the final result
         */
        public F0<R> orElse(final Func0<? extends R> fallback) {
            final F0<R> me = this;
            return new F0<R>() {
                @Override
                public R apply() {
                    try {
                        return me.apply();
                    } catch (NotAppliedException e) {
                        return fallback.apply();
                    }
                }
            };
        }

        /**
         * Turns this partial function into a plain function returning an Option result.
         *
         * @returna function that takes an argument x to Some(this.apply(x)) if this can be applied, and to None otherwise.
         */
        public F0<Option<R>> lift() {
            final F0<R> me = this;
            return new F0<Option<R>>() {
                @Override
                public Option<R> apply() {
                    try {
                        return _.some(me.apply());
                    } catch (NotAppliedException e) {
                        return _.none();
                    }
                }
            };
        }

        /**
         * Composes this partial function with an action function which gets applied to results
         * of this partial function. The action function is invoked only for its side effects;
         * its result is ignored.
         *
         * @param action the function that apply to the result of this function
         * @return a function which maps arguments x to {@code true} if this function is applied and run
         * the action function for the side effect, or {@code false} if this function is not applied.
         */
        public F0<Boolean> runWith(final F1<? super R, ?> action) {
            final F0<R> me = this;
            return new F0<Boolean>() {
                @Override
                public Boolean apply() {
                    try {
                        action.apply(me.apply());
                        return true;
                    } catch (NotAppliedException e) {
                        return false;
                    }
                }
            };
        }

    }

    /**
     * The class adapt traditional Factory to Function
     * @param <T> the type of the instance been created by the factory
     */
    public static abstract class Factory<T> extends F0<T> {

        @Override
        public T apply() throws NotAppliedException, Break {
            return create();
        }

        /**
         * The abstract create method that will be called by
         * the {@link #apply()} method
         *
         * @return an instance the factory create
         */
        public abstract T create();
    }

    private static class DumbF0 extends F0<Object> implements Serializable {
        private static final long serialVersionUID = 2856835860950L;
        @Override
        public Object apply() throws NotAppliedException, Break {
            return null;
        }
    }

    /**
     * A dumb function for {@link org.osgl._.Func0} that does nothing and return <code>null</code>
     *
     * @see #f0()
     * @since 0.2
     */
    public static final F0 F0 = new DumbF0();

    /**
     * Return a dumb function for {@link org.osgl._.Func0}. This is the type-safe version of {@link #F0}
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <T> F0<T> f0() {
        return (F0<T>)F0;
    }

    /**
     * Convert a general {@link org.osgl._.Func0} typed function to {@link F0} type
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <R> F0<R> f0(final Func0<? extends R> f0) {
        E.NPE(f0);
        if (f0 instanceof F0) {
            return (F0<R>) f0;
        }
        return new F0<R>() {
            @Override
            public R apply() {
                return f0.apply();
            }
        };
    }

    /**
     * Define a function structure that accept one parameter. This interface is created to make it
     * easily migrate to Java 8 in the future
     *
     * @param <T> the type of input parameter
     * @param <U> the type of the return value when this function applied to the parameter(s)
     * @see org.osgl._.Func0
     * @see org.osgl._.Function
     * @see org.osgl._.Func2
     * @see org.osgl._.Func3
     * @see org.osgl._.Func4
     * @see org.osgl._.Func5
     * @see F1
     * @since 0.2
     */
    public static interface Function<T, U> {

        /**
         * Apply this function to &lt;T&gt; type parameter.
         * <p/>
         * <p>In case implementing a partial function, it can throw out an
         * {@link NotAppliedException} if the function is not defined for
         * the given parameter(s)</p>
         *
         * @return {@code U} type result
         * @throws NotAppliedException if the function doesn't apply to the parameter(s)
         * @throws Break               to short cut collecting operations (fold/reduce) on an {@link org.osgl.util.C.Traversable container}
         */
        U apply(T t) throws NotAppliedException, Break;

    }

    /**
     * Alias of {@link Function}
     *
     * @param <P1>
     * @param <R>
     * @since 0.2
     */
    public static interface Func1<P1, R> extends Function<P1, R> {
    }

    /**
     * A {@link Function} function that support {@link #times(int)} operation
     *
     * @param <P1> the type of parameter the function applied to
     * @param <R>  the type of return value of the function
     */
    public static interface MultiplicableFunction<P1, R> extends Function<P1, R> {
        /**
         * Returns a function with {@code n} times factor specified. When the function
         * returned applied to a param, the effect is the same as apply this function
         * {@code n} times to the same param
         *
         * @param n specify the times factor
         * @return the new function
         */
        MultiplicableFunction<P1, R> times(int n);
    }

    /**
     * See <a href="http://en.wikipedia.org/wiki/Bijection">http://en.wikipedia.org/wiki/Bijection</a>. A
     * {@code Bijection} (mapping from {@code X} to {@code Y} is a special {@link Function} that has an
     * inverse function by itself also a {@code Bijection} mapping from {@code Y} to {@code X}
     *
     * @param <X> the type of parameter
     * @param <Y>
     */
    public static interface Bijection<X, Y> extends Function<X, Y> {
        /**
         * Returns the inverse function mapping from {@code Y} back to {@code X}
         */
        Bijection<Y, X> inverse();
    }

    /**
     * Base implementation of {@link org.osgl._.Function} function. User application should
     * (nearly) always make their implementation extend to this base class
     * instead of implement {@link org.osgl._.Function} directly
     *
     * @since 0.2
     */
    public static abstract class F1<P1, R>
            implements Func1<P1, R>, Bijection<P1, R>, MultiplicableFunction<P1, R> {

        @Override
        public Bijection<R, P1> inverse() {
            throw new NotAppliedException();
        }

        @Override
        public MultiplicableFunction<P1, R> times(int n) {
            throw new NotAppliedException();
        }

        /**
         * @see F0#breakOut(Object)
         */
        protected Break breakOut(Object payload) {
            return new Break(payload);
        }

        /**
         * Applies this partial function to the given argument when it is contained in the function domain.
         * Applies fallback function where this partial function is not defined.
         *
         * @param p1
         * @param fallback
         * @return the result of this function or the fallback function application
         */
        public R applyOrElse(P1 p1, F1<? super P1, ? extends R> fallback) {
            try {
                return apply(p1);
            } catch (NotAppliedException e) {
                return fallback.apply(p1);
            }
        }

        public final F0<R> curry(final P1 p1) {
            final Function<P1, R> me = this;
            return new F0<R>() {
                @Override
                public R apply() {
                    return me.apply(p1);
                }
            };
        }

        /**
         * Returns a composed function that applies this function to it's input and
         * then applies the {@code after} function to the result. If evaluation of either
         * function throws an exception, it is relayed to the caller of the composed
         * function.
         *
         * @param <T>   the type of the new function's application result
         * @param after the function applies after this function is applied
         * @param <T>   the type of the output of the {@code before} function
         * @return the composed function
         * @throws NullPointerException if @{code after} is null
         */
        public <T> F1<P1, T> andThen(final Function<? super R, ? extends T> after) {
            E.NPE(after);
            final Function<P1, R> me = this;
            return new F1<P1, T>() {
                @Override
                public T apply(P1 p1) {
                    return after.apply(me.apply(p1));
                }
            };
        }

        /**
         * Returns a composed function that applied, in sequence, this function and
         * all functions specified one by one. If applying anyone of the functions
         * throws an exception, it is relayed to the caller of the composed function.
         * If an exception is thrown out, the following functions will not be applied.
         * <p/>
         * <p>When apply the composed function, the result of the last function
         * is returned</p>
         *
         * @param afters a sequence of function to be applied after this function
         * @return a composed function
         */
        public F1<P1, R> andThen(final Function<? super P1, ? extends R>... afters) {
            if (0 == afters.length) {
                return this;
            }
            final F1<P1, R> me = this;
            return new F1<P1, R>() {
                @Override
                public R apply(P1 p1) {
                    R r = me.apply(p1);
                    for (Function<? super P1, ? extends R> f : afters) {
                        r = f.apply(p1);
                    }
                    return r;
                }
            };
        }

        /**
         * Returns a composed function that when applied, try to apply this function first, in case
         * a {@link NotAppliedException} is captured apply to the fallback function specified. This
         * method helps to implement partial function
         *
         * @param fallback the function to applied if this function doesn't apply to the parameter(s)
         * @return the composed function
         */
        public F1<P1, R> orElse(final Function<? super P1, ? extends R> fallback) {
            final F1<P1, R> me = this;
            return new F1<P1, R>() {
                @Override
                public R apply(P1 p1) {
                    try {
                        return me.apply(p1);
                    } catch (NotAppliedException e) {
                        return fallback.apply(p1);
                    }
                }
            };
        }

        /**
         * Returns an {@code F0&lt;R&gt;>} function by composing the specified {@code Func0&ltP1&gt;} function
         * with this function applied last
         *
         * @param before the function to be applied first when applying the return function
         * @return an new function such that f() == apply(f0())
         */
        public F0<R> compose(final Func0<? extends P1> before) {
            final F1<P1, R> me = this;
            return new F0<R>() {
                @Override
                public R apply() {
                    return me.apply(before.apply());
                }
            };
        }

        /**
         * Returns an {@code F1&lt;X1, R&gt;>} function by composing the specified
         * {@code Function&ltX1, P1&gt;} function with this function applied last
         *
         * @param before the function to be applied first when applying the return function
         * @return an new function such that f(a) == apply(f1(a))
         */
        public <X1> F1<X1, R>
        compose(final Function<? super X1, ? extends P1> before) {
            final F1<P1, R> me = this;
            return new F1<X1, R>() {
                @Override
                public R apply(X1 x1) {
                    return me.apply(before.apply(x1));
                }
            };
        }

        /**
         * Returns an {@code F2&lt;X1, X2, R&gt;>} function by composing the specified
         * {@code Func2&ltX1, X2, P1&gt;} function with this function applied last
         *
         * @param <X1>   the type of first param the new function applied to
         * @param <X2>   the type of second param the new function applied to
         * @param before the function to be applied first when applying the return function
         * @return an new function such that f(x1, x2) == apply(f1(x1, x2))
         */
        public <X1, X2> F2<X1, X2, R>
        compose(final Func2<? super X1, ? super X2, ? extends P1> before) {
            final F1<P1, R> me = this;

            return new F2<X1, X2, R>() {
                @Override
                public R apply(X1 x1, X2 x2) {
                    return me.apply(before.apply(x1, x2));
                }
            };
        }

        /**
         * Returns an {@code F3&lt;X1, X2, X3, R&gt;>} function by composing the specified
         * {@code Func3&ltX1, X2, X3, P1&gt;} function with this function applied last
         *
         * @param <X1>   the type of first param the new function applied to
         * @param <X2>   the type of second param the new function applied to
         * @param <X3>   the type of third param the new function applied to
         * @param before the function to be applied first when applying the return function
         * @return an new function such that f(x1, x2, x3) == apply(f1(x1, x2, x3))
         */
        public <X1, X2, X3> F3<X1, X2, X3, R>
        compose(final Func3<? super X1, ? super X2, ? super X3, ? extends P1> before) {
            final F1<P1, R> me = this;
            return new F3<X1, X2, X3, R>() {
                @Override
                public R apply(X1 x1, X2 x2, X3 x3) {
                    return me.apply(before.apply(x1, x2, x3));
                }
            };
        }

        /**
         * Returns an {@code F3&lt;X1, X2, X3, X4, R&gt;>} function by composing the specified
         * {@code Func3&ltX1, X2, X3, X4, P1&gt;} function with this function applied last
         *
         * @param <X1>   the type of first param the new function applied to
         * @param <X2>   the type of second param the new function applied to
         * @param <X3>   the type of third param the new function applied to
         * @param <X4>   the type of fourth param the new function applied to
         * @param before the function to be applied first when applying the return function
         * @return an new function such that f(x1, x2, x3, x4) == apply(f1(x1, x2, x3, x4))
         */
        public <X1, X2, X3, X4> F4<X1, X2, X3, X4, R>
        compose(final Func4<? super X1, ? super X2, ? super X3, ? super X4, ? extends P1> before) {
            final F1<P1, R> me = this;
            return new F4<X1, X2, X3, X4, R>() {
                @Override
                public R apply(X1 x1, X2 x2, X3 x3, X4 x4) {
                    return me.apply(before.apply(x1, x2, x3, x4));
                }
            };
        }

        /**
         * Returns an {@code F3&lt;X1, X2, X3, X4, X5, R&gt;>} function by composing the specified
         * {@code Func3&ltX1, X2, X3, X4, X5, P1&gt;} function with this function applied last
         *
         * @param <X1>   the type of first param the new function applied to
         * @param <X2>   the type of second param the new function applied to
         * @param <X3>   the type of third param the new function applied to
         * @param <X4>   the type of fourth param the new function applied to
         * @param <X5>   the type of fifth param the new function applied to
         * @param before the function to be applied first when applying the return function
         * @return an new function such that f(x1, x2, x3, x4, x5) == apply(f1(x1, x2, x3, x4, x5))
         */
        public <X1, X2, X3, X4, X5> F5<X1, X2, X3, X4, X5, R>
        compose(final Func5<? super X1, ? super X2, ? super X3, ? super X4, ? super X5, ? extends P1> before) {
            final F1<P1, R> me = this;
            return new F5<X1, X2, X3, X4, X5, R>() {
                @Override
                public R apply(X1 x1, X2 x2, X3 x3, X4 x4, X5 x5) {
                    return me.apply(before.apply(x1, x2, x3, x4, x5));
                }
            };
        }

        /**
         * Turns this partial function into a plain function returning an Option result.
         *
         * @returna function that takes an argument x to Some(this.apply(x)) if this can be applied, and to None otherwise.
         */
        public F1<P1, Option<R>> lift() {
            final F1<P1, R> me = this;
            return new F1<P1, Option<R>>() {
                @Override
                public Option<R> apply(P1 p1) {
                    try {
                        return _.some(me.apply(p1));
                    } catch (NotAppliedException e) {
                        return _.none();
                    }
                }
            };
        }

        /**
         * Composes this partial function with an action function which gets applied to results
         * of this partial function. The action function is invoked only for its side effects;
         * its result is ignored.
         *
         * @param action the function that apply to the result of this function
         * @return a function which maps arguments x to {@code true} if this function is applied and run
         * the action function for the side effect, or {@code false} if this function is not applied.
         */
        public F1<P1, Boolean> runWith(final F1<? super R, ?> action) {
            final F1<P1, R> me = this;
            return new F1<P1, Boolean>() {
                @Override
                public Boolean apply(P1 p1) {
                    try {
                        action.apply(me.apply(p1));
                        return true;
                    } catch (NotAppliedException e) {
                        return false;
                    }
                }
            };
        }
    }

    private static class DumbF1 extends F1<Object, Object> implements Serializable {
        private static final long serialVersionUID = 2856835860951L;
        @Override
        public Object apply(Object o) throws NotAppliedException, Break {
            return null;
        }
    }

    /**
     * A dumb {@link org.osgl._.Function} implementation that does nothing and return null
     *
     * @see #f1()
     * @since 0.2
     */
    public static final F1 F1 = new DumbF1();

    /**
     * The type-safe version of {@link #F1}
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <P1, R> F1<P1, R> f1() {
        return (F1<P1, R>)F1;
    }


    /**
     * Convert a general {@link org.osgl._.Function} function into a {@link F1} typed
     * function
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <P1, R> F1<P1, R> f1(final Function<? super P1, ? extends R> f1) {
        E.NPE(f1);
        if (f1 instanceof F1) {
            return (F1<P1, R>) f1;
        }
        return new F1<P1, R>() {
            @Override
            public R apply(P1 p1) {
                return f1.apply(p1);
            }

            @Override
            public MultiplicableFunction<P1, R> times(int n) {
                if (f1 instanceof MultiplicableFunction) {
                    return ((MultiplicableFunction<P1, R>) f1).times(n);
                }
                return super.times(n);
            }
        };
    }

    public static <X, Y> F1<X, Y> f1(final Bijection<X, Y> f1) {
        E.NPE(f1);
        if (f1 instanceof F1) {
            return (F1<X, Y>) f1;
        }
        return new F1<X, Y>() {
            @Override
            public Y apply(X p1) {
                return f1.apply(p1);
            }

            @Override
            public F1<Y, X> inverse() {
                return _.f1(f1.inverse());
            }

            @Override
            public MultiplicableFunction<X, Y> times(int n) {
                if (f1 instanceof MultiplicableFunction) {
                    return ((MultiplicableFunction<X, Y>) f1).times(n);
                }
                return super.times(n);
            }
        };
    }

    /**
     * Define a function structure that accept two parameter
     *
     * @param <P1> the type of first parameter this function applied to
     * @param <P2> the type of second parameter this function applied to
     * @param <R>  the type of the return value when this function applied to the parameter(s)
     * @see org.osgl._.Func0
     * @see org.osgl._.Function
     * @see org.osgl._.Func3
     * @see org.osgl._.Func4
     * @see org.osgl._.Func5
     * @see F2
     * @since 0.2
     */
    public static interface Func2<P1, P2, R> {

        /**
         * Apply the two params to the function
         * <p/>
         * <p>In case implementing a partial function, it can throw out an
         * {@link NotAppliedException} if the function is not defined for
         * the given parameter(s)</p>
         *
         * @throws NotAppliedException if the function doesn't apply to the parameter(s)
         * @throws Break               to short cut collecting operations (fold/reduce) on an {@link org.osgl.util.C.Traversable container}
         */
        R apply(P1 p1, P2 p2) throws NotAppliedException, Break;

    }

    /**
     * Base implementation of {@link org.osgl._.Func2} function. User application should
     * (nearly) always make their implementation extend to this base class
     * instead of implement {@link org.osgl._.Func2} directly
     *
     * @since 0.2
     */
    public static abstract class F2<P1, P2, R> implements Func2<P1, P2, R> {

        protected Break breakOut(Object payload) {
            return new Break(payload);
        }

        /**
         * Applies this partial function to the given argument when it is contained in the function domain.
         * Applies fallback function where this partial function is not defined.
         *
         * @param p1
         * @param p2
         * @param fallback
         * @return the result of this function or the fallback function application
         */
        public R applyOrElse(P1 p1, P2 p2, F2<? super P1, ? super P2, ? extends R> fallback) {
            try {
                return apply(p1, p2);
            } catch (NotAppliedException e) {
                return fallback.apply(p1, p2);
            }
        }

        public final F0<R> curry(final P1 p1, final P2 p2) {
            final F2<P1, P2, R> me = this;
            return new F0<R>() {
                @Override
                public R apply() {
                    return me.apply(p1, p2);
                }
            };
        }

        public final F1<P1, R> curry(final P2 p2) {
            final F2<P1, P2, R> me = this;
            return new F1<P1, R>() {
                @Override
                public R apply(P1 p1) {
                    return me.apply(p1, p2);
                }
            };
        }

        /**
         * Returns a composed function from this function and the specified function that takes the
         * result of this function. When applying the composed function, this function is applied
         * first to given parameter and then the specified function is applied to the result of
         * this function.
         *
         * @param f the function takes the <code>R</code> type parameter and return <code>T</code>
         *          type result
         * @return the composed function
         * @throws NullPointerException if <code>f</code> is null
         */
        public <T> F2<P1, P2, T> andThen(final Function<? super R, ? extends T> f) {
            E.NPE(f);
            final Func2<P1, P2, R> me = this;
            return new F2<P1, P2, T>() {
                @Override
                public T apply(P1 p1, P2 p2) {
                    R r = me.apply(p1, p2);
                    return f.apply(r);
                }
            };
        }

        /**
         * Returns a composed function that applied, in sequence, this function and
         * all functions specified one by one. If applying anyone of the functions
         * throws an exception, it is relayed to the caller of the composed function.
         * If an exception is thrown out, the following functions will not be applied.
         * <p/>
         * <p>When apply the composed function, the result of the last function
         * is returned</p>
         *
         * @param fs a sequence of function to be applied after this function
         * @return a composed function
         */
        public F2<P1, P2, R> andThen(final Func2<? super P1, ? super P2, ? extends R>... fs) {
            if (0 == fs.length) {
                return this;
            }
            final Func2<P1, P2, R> me = this;
            return new F2<P1, P2, R>() {
                @Override
                public R apply(P1 p1, P2 p2) {
                    R r = me.apply(p1, p2);
                    for (Func2<? super P1, ? super P2, ? extends R> f : fs) {
                        r = f.apply(p1, p2);
                    }
                    return r;
                }
            };
        }

        /**
         * Returns a composed function that when applied, try to apply this function first, in case
         * a {@link NotAppliedException} is captured apply to the fallback function specified. This
         * method helps to implement partial function
         *
         * @param fallback the function to applied if this function doesn't apply to the parameter(s)
         * @return the composed function
         */
        public F2<P1, P2, R> orElse(final Func2<? super P1, ? super P2, ? extends R> fallback) {
            final F2<P1, P2, R> me = this;
            return new F2<P1, P2, R>() {
                @Override
                public R apply(P1 p1, P2 p2) {
                    try {
                        return me.apply(p1, p2);
                    } catch (NotAppliedException e) {
                        return fallback.apply(p1, p2);
                    }
                }
            };
        }

        /**
         * Turns this partial function into a plain function returning an Option result.
         *
         * @returna function that takes an argument x to Some(this.apply(x)) if this can be applied, and to None otherwise.
         */
        public F2<P1, P2, Option<R>> lift() {
            final F2<P1, P2, R> me = this;
            return new F2<P1, P2, Option<R>>() {
                @Override
                public Option<R> apply(P1 p1, P2 p2) {
                    try {
                        return _.some(me.apply(p1, p2));
                    } catch (NotAppliedException e) {
                        return _.none();
                    }
                }
            };
        }

        /**
         * Composes this partial function with an action function which gets applied to results
         * of this partial function. The action function is invoked only for its side effects;
         * its result is ignored.
         *
         * @param action the function that apply to the result of this function
         * @return a function which maps arguments x to {@code true} if this function is applied and run
         * the action function for the side effect, or {@code false} if this function is not applied.
         */
        public F2<P1, P2, Boolean> runWith(final F1<? super R, ?> action) {
            final F2<P1, P2, R> me = this;
            return new F2<P1, P2, Boolean>() {
                @Override
                public Boolean apply(P1 p1, P2 p2) {
                    try {
                        action.apply(me.apply(p1, p2));
                        return true;
                    } catch (NotAppliedException e) {
                        return false;
                    }
                }
            };
        }
    }

    private static class DumbF2 extends F2<Object, Object, Object> implements Serializable {
        private static final long serialVersionUID = 2856835860952L;

        @Override
        public Object apply(Object o, Object o2) throws NotAppliedException, Break {
            return null;
        }
    }

    /**
     * A dumb {@link org.osgl._.Func2} implementation that does nothing and return null
     *
     * @see #f2()
     * @since 0.2
     */
    public static final F2 F2 = new DumbF2();

    /**
     * The type-safe version of {@link #F2}
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <P1, P2, R> F2<P1, P2, R> f2() {
        return (F2<P1, P2, R>)F2;
    }


    /**
     * Convert a general {@link org.osgl._.Func2} function into a {@link F2} typed
     * function
     *
     * @param <P1> the type of the first param the new function applied to
     * @param <P2> the type of the second param the new function applied to
     * @param <R>  the type of new function application result
     * @return a {@code F2} instance corresponding to the specified {@code Func2} instance
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <P1, P2, R> F2<P1, P2, R> f2(final Func2<? super P1, ? super P2, ? extends R> f2) {
        E.NPE(f2);
        if (f2 instanceof F2) {
            return (F2<P1, P2, R>) f2;
        }
        return new F2<P1, P2, R>() {
            @Override
            public R apply(P1 p1, P2 p2) {
                return f2.apply(p1, p2);
            }

        };
    }

    /**
     * Define a function structure that accept three parameter
     *
     * @param <P1> the type of first parameter this function applied to
     * @param <P2> the type of second parameter this function applied to
     * @param <P3> the type of thrid parameter this function applied to
     * @param <R>  the type of the return value when this function applied to the parameter(s)
     * @see org.osgl._.Func0
     * @see org.osgl._.Function
     * @see org.osgl._.Func2
     * @see org.osgl._.Func4
     * @see org.osgl._.Func5
     * @since 0.2
     */
    public static interface Func3<P1, P2, P3, R> {
        /**
         * Run the function with parameters specified.
         * <p/>
         * <p>In case implementing a partial function, it can throw out an
         * {@link NotAppliedException} if the function is not defined for
         * the given parameter(s)</p>
         *
         * @throws NotAppliedException if the function doesn't apply to the parameter(s)
         * @throws Break               to short cut collecting operations (fold/reduce) on an {@link org.osgl.util.C.Traversable container}
         */
        R apply(P1 p1, P2 p2, P3 p3) throws NotAppliedException, Break;

    }

    /**
     * Base implementation of {@link org.osgl._.Func3} function. User application should
     * (nearly) always make their implementation extend to this base class
     * instead of implement {@link org.osgl._.Func3} directly
     *
     * @since 0.2
     */
    public static abstract class F3<P1, P2, P3, R> implements Func3<P1, P2, P3, R> {
        /**
         * @see F1#breakOut(Object)
         */
        protected Break breakOut(Object payload) {
            return new Break(payload);
        }

        /**
         * Applies this partial function to the given argument when it is contained in the function domain.
         * Applies fallback function where this partial function is not defined.
         *
         * @param p1
         * @param p2
         * @param p3
         * @param fallback
         * @return the result of this function or the fallback function application
         */
        public R applyOrElse(P1 p1, P2 p2, P3 p3, F3<? super P1, ? super P2, ? super P3, ? extends R> fallback) {
            try {
                return apply(p1, p2, p3);
            } catch (NotAppliedException e) {
                return fallback.apply(p1, p2, p3);
            }
        }

        public final F0<R> curry(final P1 p1, final P2 p2, final P3 p3) {
            final F3<P1, P2, P3, R> me = this;
            return new F0<R>() {
                @Override
                public R apply() {
                    return me.apply(p1, p2, p3);
                }
            };
        }

        public final F1<P1, R> curry(final P2 p2, final P3 p3) {
            final F3<P1, P2, P3, R> me = this;
            return new F1<P1, R>() {
                @Override
                public R apply(P1 p1) {
                    return me.apply(p1, p2, p3);
                }
            };
        }

        public final F2<P1, P2, R> curry(final P3 p3) {
            final F3<P1, P2, P3, R> me = this;
            return new F2<P1, P2, R>() {
                @Override
                public R apply(P1 p1, P2 p2) {
                    return me.apply(p1, p2, p3);
                }
            };
        }

        /**
         * Returns a composed function from this function and the specified function that takes the
         * result of this function. When applying the composed function, this function is applied
         * first to given parameter and then the specified function is applied to the result of
         * this function.
         *
         * @param f the function takes the <code>R</code> type parameter and return <code>T</code>
         *          type result
         * @return the composed function
         * @throws NullPointerException if <code>f</code> is null
         */
        public <T> Func3<P1, P2, P3, T> andThen(final Function<? super R, ? extends T> f) {
            E.NPE(f);
            final Func3<P1, P2, P3, R> me = this;
            return new F3<P1, P2, P3, T>() {
                @Override
                public T apply(P1 p1, P2 p2, P3 p3) {
                    R r = me.apply(p1, p2, p3);
                    return f.apply(r);
                }
            };
        }

        /**
         * Returns a composed function that applied, in sequence, this function and
         * all functions specified one by one. If applying anyone of the functions
         * throws an exception, it is relayed to the caller of the composed function.
         * If an exception is thrown out, the following functions will not be applied.
         * <p/>
         * <p>When apply the composed function, the result of the last function
         * is returned</p>
         *
         * @param fs a sequence of function to be applied after this function
         * @return a composed function
         */
        public Func3<P1, P2, P3, R> andThen(
                final Func3<? super P1, ? super P2, ? super P3, ? extends R>... fs
        ) {
            if (0 == fs.length) {
                return this;
            }

            final Func3<P1, P2, P3, R> me = this;
            return new F3<P1, P2, P3, R>() {
                @Override
                public R apply(P1 p1, P2 p2, P3 p3) {
                    R r = me.apply(p1, p2, p3);
                    for (Func3<? super P1, ? super P2, ? super P3, ? extends R> f : fs) {
                        r = f.apply(p1, p2, p3);
                    }
                    return r;
                }
            };
        }

        /**
         * Returns a composed function that when applied, try to apply this function first, in case
         * a {@link NotAppliedException} is captured apply to the fallback function specified. This
         * method helps to implement partial function
         *
         * @param fallback the function to applied if this function doesn't apply to the parameter(s)
         * @return the composed function
         */
        public F3<P1, P2, P3, R> orElse(final Func3<? super P1, ? super P2, ? super P3, ? extends R> fallback) {
            final F3<P1, P2, P3, R> me = this;
            return new F3<P1, P2, P3, R>() {
                @Override
                public R apply(P1 p1, P2 p2, P3 p3) {
                    try {
                        return me.apply(p1, p2, p3);
                    } catch (NotAppliedException e) {
                        return fallback.apply(p1, p2, p3);
                    }
                }
            };
        }

        /**
         * Turns this partial function into a plain function returning an Option result.
         *
         * @returna function that takes an argument x to Some(this.apply(x)) if this can be applied, and to None otherwise.
         */
        public F3<P1, P2, P3, Option<R>> lift() {
            final F3<P1, P2, P3, R> me = this;
            return new F3<P1, P2, P3, Option<R>>() {
                @Override
                public Option<R> apply(P1 p1, P2 p2, P3 p3) {
                    try {
                        return _.some(me.apply(p1, p2, p3));
                    } catch (NotAppliedException e) {
                        return _.none();
                    }
                }
            };
        }

        /**
         * Composes this partial function with an action function which gets applied to results
         * of this partial function. The action function is invoked only for its side effects;
         * its result is ignored.
         *
         * @param action the function that apply to the result of this function
         * @return a function which maps arguments x to {@code true} if this function is applied and run
         * the action function for the side effect, or {@code false} if this function is not applied.
         */
        public F3<P1, P2, P3, Boolean> runWith(final F1<? super R, ?> action) {
            final F3<P1, P2, P3, R> me = this;
            return new F3<P1, P2, P3, Boolean>() {
                @Override
                public Boolean apply(P1 p1, P2 p2, P3 p3) {
                    try {
                        action.apply(me.apply(p1, p2, p3));
                        return true;
                    } catch (NotAppliedException e) {
                        return false;
                    }
                }
            };
        }
    }


    private static class DumbF3 extends F3<Object, Object, Object, Object> implements Serializable {
        private static final long serialVersionUID = 2856835860953L;

        @Override
        public Object apply(Object o, Object o2, Object o3) throws NotAppliedException, Break {
            return null;
        }
    }

    /**
     * A dumb {@link org.osgl._.Func3} implementation that does nothing and return null
     *
     * @see #f3()
     * @since 0.2
     */
    public static final F3 F3 = new DumbF3();

    /**
     * The type-safe version of {@link #F3}
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <P1, P2, P3, R> F3<P1, P2, P3, R> f3() {
        return F3;
    }


    /**
     * Convert a general {@link org.osgl._.Func3} function into a {@link F3} typed
     * function
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <P1, P2, P3, R> F3<P1, P2, P3, R> f3(final Func3<? super P1, ? super P2, ? super P3, ? extends R> f3
    ) {
        E.NPE(f3);
        if (f3 instanceof F3) {
            return (F3<P1, P2, P3, R>) f3;
        }
        return new F3<P1, P2, P3, R>() {
            @Override
            public R apply(P1 p1, P2 p2, P3 p3) {
                return f3.apply(p1, p2, p3);
            }
        };
    }

    /**
     * Define a function structure that accept four parameter
     *
     * @param <P1> the type of first parameter this function applied to
     * @param <P2> the type of second parameter this function applied to
     * @param <P3> the type of thrid parameter this function applied to
     * @param <P4> the type of fourth parameter this function applied to
     * @param <R>  the type of the return value when this function applied to the parameter(s)
     * @see org.osgl._.Func0
     * @see org.osgl._.Function
     * @see org.osgl._.Func2
     * @see org.osgl._.Func4
     * @see org.osgl._.Func5
     * @since 0.2
     */
    public static interface Func4<P1, P2, P3, P4, R> {
        /**
         * Run the function with parameters specified.
         * <p/>
         * <p>In case implementing a partial function, it can throw out an
         * {@link NotAppliedException} if the function is not defined for
         * the given parameter(s)</p>
         *
         * @throws NotAppliedException if the function doesn't apply to the parameter(s)
         * @throws Break               to short cut collecting operations (fold/reduce) on an {@link org.osgl.util.C.Traversable container}
         */
        R apply(P1 p1, P2 p2, P3 p3, P4 p4) throws NotAppliedException, Break;

    }

    /**
     * Base implementation of {@link org.osgl._.Func4} function. User application should
     * (nearly) always make their implementation extend to this base class
     * instead of implement {@link org.osgl._.Func4} directly
     *
     * @since 0.2
     */
    public static abstract class F4<P1, P2, P3, P4, R> implements Func4<P1, P2, P3, P4, R> {
        /**
         * @see F1#breakOut(Object)
         */
        public Break breakOut(Object payload) {
            return new Break(payload);
        }

        /**
         * Applies this partial function to the given argument when it is contained in the function domain.
         * Applies fallback function where this partial function is not defined.
         *
         * @param p1
         * @param p2
         * @param p3
         * @param p4
         * @param fallback
         * @return the result of this function or the fallback function application
         */
        public R applyOrElse(P1 p1, P2 p2, P3 p3, P4 p4,
                             F4<? super P1, ? super P2, ? super P3, ? super P4, ? extends R> fallback
        ) {
            try {
                return apply(p1, p2, p3, p4);
            } catch (NotAppliedException e) {
                return fallback.apply(p1, p2, p3, p4);
            }
        }

        public final F0<R> curry(final P1 p1, final P2 p2, final P3 p3, final P4 p4) {
            final F4<P1, P2, P3, P4, R> me = this;
            return new F0<R>() {
                @Override
                public R apply() {
                    return me.apply(p1, p2, p3, p4);
                }
            };
        }

        public final F1<P1, R> curry(final P2 p2, final P3 p3, final P4 p4) {
            final F4<P1, P2, P3, P4, R> me = this;
            return new F1<P1, R>() {
                @Override
                public R apply(P1 p1) {
                    return me.apply(p1, p2, p3, p4);
                }
            };
        }

        public final F2<P1, P2, R> curry(final P3 p3, final P4 p4) {
            final F4<P1, P2, P3, P4, R> me = this;
            return new F2<P1, P2, R>() {
                @Override
                public R apply(P1 p1, P2 p2) {
                    return me.apply(p1, p2, p3, p4);
                }
            };
        }

        public final F3<P1, P2, P3, R> curry(final P4 p4) {
            final F4<P1, P2, P3, P4, R> me = this;
            return new F3<P1, P2, P3, R>() {
                @Override
                public R apply(P1 p1, P2 p2, P3 p3) {
                    return me.apply(p1, p2, p3, p4);
                }
            };
        }

        /**
         * Returns a composed function from this function and the specified function that takes the
         * result of this function. When applying the composed function, this function is applied
         * first to given parameter and then the specified function is applied to the result of
         * this function.
         *
         * @param f the function takes the <code>R</code> type parameter and return <code>T</code>
         *          type result
         * @return the composed function
         * @throws NullPointerException if <code>f</code> is null
         */
        public <T> F4<P1, P2, P3, P4, T> andThen(final Function<? super R, ? extends T> f) {
            E.NPE(f);
            final Func4<P1, P2, P3, P4, R> me = this;
            return new F4<P1, P2, P3, P4, T>() {
                @Override
                public T apply(P1 p1, P2 p2, P3 p3, P4 p4) {
                    R r = me.apply(p1, p2, p3, p4);
                    return f.apply(r);
                }
            };
        }

        /**
         * Returns a composed function that applied, in sequence, this function and
         * all functions specified one by one. If applying anyone of the functions
         * throws an exception, it is relayed to the caller of the composed function.
         * If an exception is thrown out, the following functions will not be applied.
         * <p/>
         * <p>When apply the composed function, the result of the last function
         * is returned</p>
         *
         * @param fs a sequence of function to be applied after this function
         * @return a composed function
         */
        public F4<P1, P2, P3, P4, R> andThen(
                final Func4<? super P1, ? super P2, ? super P3, ? super P4, ? extends R>... fs
        ) {
            if (0 == fs.length) {
                return this;
            }

            final Func4<P1, P2, P3, P4, R> me = this;
            return new F4<P1, P2, P3, P4, R>() {
                @Override
                public R apply(P1 p1, P2 p2, P3 p3, P4 p4) {
                    R r = me.apply(p1, p2, p3, p4);
                    for (Func4<? super P1, ? super P2, ? super P3, ? super P4, ? extends R> f : fs) {
                        r = f.apply(p1, p2, p3, p4);
                    }
                    return r;
                }
            };
        }

        /**
         * Returns a composed function that when applied, try to apply this function first, in case
         * a {@link NotAppliedException} is captured apply to the fallback function specified. This
         * method helps to implement partial function
         *
         * @param fallback the function to applied if this function doesn't apply to the parameter(s)
         * @return the composed function
         */
        public F4<P1, P2, P3, P4, R> orElse(
                final Func4<? super P1, ? super P2, ? super P3, ? super P4, ? extends R> fallback
        ) {
            final F4<P1, P2, P3, P4, R> me = this;
            return new F4<P1, P2, P3, P4, R>() {
                @Override
                public R apply(P1 p1, P2 p2, P3 p3, P4 p4) {
                    try {
                        return me.apply(p1, p2, p3, p4);
                    } catch (NotAppliedException e) {
                        return fallback.apply(p1, p2, p3, p4);
                    }
                }
            };
        }

        /**
         * Turns this partial function into a plain function returning an Option result.
         *
         * @return a function that takes an argument x to Some(this.apply(x)) if this can be applied, and to None otherwise.
         */
        public F4<P1, P2, P3, P4, Option<R>> lift() {
            final F4<P1, P2, P3, P4, R> me = this;
            return new F4<P1, P2, P3, P4, Option<R>>() {
                @Override
                public Option<R> apply(P1 p1, P2 p2, P3 p3, P4 p4) {
                    try {
                        return _.some(me.apply(p1, p2, p3, p4));
                    } catch (NotAppliedException e) {
                        return _.none();
                    }
                }
            };
        }

        /**
         * Composes this partial function with an action function which gets applied to results
         * of this partial function. The action function is invoked only for its side effects;
         * its result is ignored.
         *
         * @param action the function that apply to the result of this function
         * @return a function which maps arguments x to {@code true} if this function is applied and run
         * the action function for the side effect, or {@code false} if this function is not applied.
         */
        public F4<P1, P2, P3, P4, Boolean> runWith(final F1<? super R, ?> action) {
            final F4<P1, P2, P3, P4, R> me = this;
            return new F4<P1, P2, P3, P4, Boolean>() {
                @Override
                public Boolean apply(P1 p1, P2 p2, P3 p3, P4 p4) {
                    try {
                        action.apply(me.apply(p1, p2, p3, p4));
                        return true;
                    } catch (NotAppliedException e) {
                        return false;
                    }
                }
            };
        }
    }

    private static class DumbF4 extends F4<Object, Object, Object, Object, Object> implements Serializable {
        private static final long serialVersionUID = 2856835860954L;

        @Override
        public Object apply(Object o, Object o2, Object o3, Object o4) throws NotAppliedException, Break {
            return null;
        }
    }

    /**
     * A dumb {@link org.osgl._.Func4} implementation that does nothing and return null
     *
     * @see #f4()
     * @since 0.2
     */
    public static final F4 F4 = new DumbF4();

    /**
     * The type-safe version of {@link #F4}
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <P1, P2, P3, P4, R> F4<P1, P2, P3, P4, R> f4() {
        return F4;
    }


    /**
     * Convert a general {@link org.osgl._.Func4} function into a {@link F4} typed
     * function
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <P1, P2, P3, P4, R> F4<P1, P2, P3, P4, R> f4(
            final Func4<? super P1, ? super P2, ? super P3, ? super P4, ? extends R> f4
    ) {
        E.NPE(f4);
        if (f4 instanceof F4) {
            return (F4<P1, P2, P3, P4, R>) f4;
        }
        return new F4<P1, P2, P3, P4, R>() {
            @Override
            public R apply(P1 p1, P2 p2, P3 p3, P4 p4) {
                return f4.apply(p1, p2, p3, p4);
            }
        };
    }

    /**
     * Define a function structure that accept five parameter
     *
     * @param <P1> the type of first parameter this function applied to
     * @param <P2> the type of second parameter this function applied to
     * @param <P3> the type of thrid parameter this function applied to
     * @param <P4> the type of fourth parameter this function applied to
     * @param <P5> the type of fifth parameter this function applied to
     * @param <R>  the type of the return value when this function applied to the parameter(s)
     * @see org.osgl._.Func0
     * @see org.osgl._.Function
     * @see org.osgl._.Func2
     * @see org.osgl._.Func3
     * @see org.osgl._.Func5
     * @since 0.2
     */
    public static interface Func5<P1, P2, P3, P4, P5, R> {
        /**
         * Run the function with parameters specified.
         * <p/>
         * <p>In case implementing a partial function, it can throw out an
         * {@link NotAppliedException} if the function is not defined for
         * the given parameter(s)</p>
         *
         * @throws NotAppliedException if the function doesn't apply to the parameter(s)
         * @throws Break               to short cut collecting operations (fold/reduce) on an {@link org.osgl.util.C.Traversable container}
         */
        R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) throws NotAppliedException, Break;
    }

    /**
     * Base implementation of {@link org.osgl._.Func5} function. User application should
     * (nearly) always make their implementation extend to this base class
     * instead of implement {@link org.osgl._.Func5} directly
     *
     * @since 0.2
     */
    public static abstract class F5<P1, P2, P3, P4, P5, R> implements Func5<P1, P2, P3, P4, P5, R> {
        /**
         * @see F1#breakOut(Object)
         */
        public Break breakOut(Object payload) {
            return new Break(payload);
        }

        /**
         * Applies this partial function to the given argument when it is contained in the function domain.
         * Applies fallback function where this partial function is not defined.
         *
         * @param p1
         * @param p2
         * @param p3
         * @param p4
         * @param p5
         * @param fallback
         * @return the result of this function or the fallback function application
         */
        public R applyOrElse(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5,
                             F5<? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? extends R> fallback
        ) {
            try {
                return apply(p1, p2, p3, p4, p5);
            } catch (NotAppliedException e) {
                return fallback.apply(p1, p2, p3, p4, p5);
            }
        }

        public final F0<R> curry(final P1 p1, final P2 p2, final P3 p3, final P4 p4, final P5 p5) {
            final F5<P1, P2, P3, P4, P5, R> f5 = this;
            return new F0<R>() {
                @Override
                public R apply() {
                    return f5.apply(p1, p2, p3, p4, p5);
                }
            };
        }

        public final F1<P1, R> curry(final P2 p2, final P3 p3, final P4 p4, final P5 p5) {
            final F5<P1, P2, P3, P4, P5, R> f5 = this;
            return new F1<P1, R>() {
                @Override
                public R apply(P1 p1) {
                    return f5.apply(p1, p2, p3, p4, p5);
                }
            };
        }

        public final F2<P1, P2, R> curry(final P3 p3, final P4 p4, final P5 p5) {
            final F5<P1, P2, P3, P4, P5, R> f5 = this;
            return new F2<P1, P2, R>() {
                @Override
                public R apply(P1 p1, P2 p2) {
                    return f5.apply(p1, p2, p3, p4, p5);
                }
            };
        }

        public final F3<P1, P2, P3, R> curry(final P4 p4, final P5 p5) {
            final F5<P1, P2, P3, P4, P5, R> f5 = this;
            return new F3<P1, P2, P3, R>() {
                @Override
                public R apply(P1 p1, P2 p2, P3 p3) {
                    return f5.apply(p1, p2, p3, p4, p5);
                }
            };
        }

        public final F4<P1, P2, P3, P4, R> curry(final P5 p5) {
            final F5<P1, P2, P3, P4, P5, R> f5 = this;
            return new F4<P1, P2, P3, P4, R>() {
                @Override
                public R apply(P1 p1, P2 p2, P3 p3, P4 p4) {
                    return f5.apply(p1, p2, p3, p4, p5);
                }
            };
        }

        /**
         * Returns a composed function from this function and the specified function that takes the
         * result of this function. When applying the composed function, this function is applied
         * first to given parameter and then the specified function is applied to the result of
         * this function.
         *
         * @param f the function takes the <code>R</code> type parameter and return <code>T</code>
         *          type result
         * @return the composed function
         * @throws NullPointerException if <code>f</code> is null
         */
        public <T> F5<P1, P2, P3, P4, P5, T> andThen(final Function<? super R, ? extends T> f) {
            E.NPE(f);
            final F5<P1, P2, P3, P4, P5, R> me = this;
            return new F5<P1, P2, P3, P4, P5, T>() {
                @Override
                public T apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
                    R r = me.apply(p1, p2, p3, p4, p5);
                    return f.apply(r);
                }
            };
        }

        /**
         * Returns a composed function that applied, in sequence, this function and
         * all functions specified one by one. If applying anyone of the functions
         * throws an exception, it is relayed to the caller of the composed function.
         * If an exception is thrown out, the following functions will not be applied.
         * <p/>
         * <p>When apply the composed function, the result of the last function
         * is returned</p>
         *
         * @param fs a sequence of function to be applied after this function
         * @return a composed function
         */
        public F5<P1, P2, P3, P4, P5, R> andThen(
                final Func5<? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? extends R>... fs
        ) {
            if (0 == fs.length) {
                return this;
            }

            final F5<P1, P2, P3, P4, P5, R> me = this;
            return new F5<P1, P2, P3, P4, P5, R>() {
                @Override
                public R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
                    R r = me.apply(p1, p2, p3, p4, p5);
                    for (Func5<? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? extends R> f : fs) {
                        r = f.apply(p1, p2, p3, p4, p5);
                    }
                    return r;
                }
            };
        }

        /**
         * Returns a composed function that when applied, try to apply this function first, in case
         * a {@link NotAppliedException} is captured apply to the fallback function specified. This
         * method helps to implement partial function
         *
         * @param fallback the function to applied if this function doesn't apply to the parameter(s)
         * @return the composed function
         */
        public F5<P1, P2, P3, P4, P5, R> orElse(
                final Func5<? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? extends R> fallback
        ) {
            final F5<P1, P2, P3, P4, P5, R> me = this;
            return new F5<P1, P2, P3, P4, P5, R>() {
                @Override
                public R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
                    try {
                        return me.apply(p1, p2, p3, p4, p5);
                    } catch (NotAppliedException e) {
                        return fallback.apply(p1, p2, p3, p4, p5);
                    }
                }
            };
        }

        /**
         * Turns this partial function into a plain function returning an Option result.
         *
         * @return a function that takes an argument x to Some(this.apply(x)) if this can be applied, and to None otherwise.
         */
        public F5<P1, P2, P3, P4, P5, Option<R>> lift() {
            final F5<P1, P2, P3, P4, P5, R> me = this;
            return new F5<P1, P2, P3, P4, P5, Option<R>>() {
                @Override
                public Option<R> apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
                    try {
                        return _.some(me.apply(p1, p2, p3, p4, p5));
                    } catch (NotAppliedException e) {
                        return _.none();
                    }
                }
            };
        }

        /**
         * Composes this partial function with an action function which gets applied to results
         * of this partial function. The action function is invoked only for its side effects;
         * its result is ignored.
         *
         * @param action the function that apply to the result of this function
         * @return a function which maps arguments x to {@code true} if this function is applied and run
         * the action function for the side effect, or {@code false} if this function is not applied.
         */
        public F5<P1, P2, P3, P4, P5, Boolean> runWith(final F1<? super R, ?> action) {
            final F5<P1, P2, P3, P4, P5, R> me = this;
            return new F5<P1, P2, P3, P4, P5, Boolean>() {
                @Override
                public Boolean apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
                    try {
                        action.apply(me.apply(p1, p2, p3, p4, p5));
                        return true;
                    } catch (NotAppliedException e) {
                        return false;
                    }
                }
            };
        }
    }

    private static class DumbF5 extends F5<Object, Object, Object, Object, Object, Object> implements Serializable {
        private static final long serialVersionUID = 2856835860955L;

        @Override
        public Object apply(Object o, Object o2, Object o3, Object o4, Object o5) throws NotAppliedException, Break {
            return null;
        }
    }

    /**
     * A dumb {@link org.osgl._.Func5} implementation that does nothing and return null
     *
     * @see #f5()
     * @since 0.2
     */
    public static final F5 F5 = new DumbF5();

    /**
     * The type-safe version of {@link #F5}
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <P1, P2, P3, P4, P5, R> F5<P1, P2, P3, P4, P5, R> f5() {
        return F5;
    }


    /**
     * Convert a general {@link org.osgl._.Func5} function into a {@link F5} typed
     * function
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <P1, P2, P3, P4, P5, R> F5<P1, P2, P3, P4, P5, R> f5(
            final Func5<? super P1, ? super P2, ? super P3, ? super P4, ? super P5, ? extends R> f5
    ) {
        E.NPE(f5);
        if (f5 instanceof F5) {
            return (F5<P1, P2, P3, P4, P5, R>) f5;
        }
        return new F5<P1, P2, P3, P4, P5, R>() {
            @Override
            public R apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
                return f5.apply(p1, p2, p3, p4, p5);
            }
        };
    }

    // Define common used Function classes including Predicate and Visitor

    /**
     * Adapt JDK Comparator (since 1.2) to Functional programming. The class provides several java8 Comparator methods
     *
     * @param <T> the type of the element to be compared
     * @since 0.2
     */
    public static abstract class Comparator<T>
            extends F2<T, T, Integer>
            implements java.util.Comparator<T>, Serializable {

        private static class NaturalOrderComparator extends Comparator<Comparable<Object>> implements Serializable {
            static final NaturalOrderComparator INSTANCE = new NaturalOrderComparator();

            @Override
            public int compare(Comparable<Object> c1, Comparable<Object> c2) {
                return c1.compareTo(c2);
            }

            private Object readResolve() {
                return INSTANCE;
            }

            @Override
            public Comparator<Comparable<Object>> reversed() {
                return _.F.reverseOrder();
            }
        }

        @Override
        public Integer apply(T p1, T p2) throws NotAppliedException, Break {
            return compare(p1, p2);
        }

        /**
         * See <a href="http://download.java.net/jdk8/docs/api/java/util/Comparator.html#reversed()">Java 8 doc</a>
         *
         * @since 0.2
         */
        public Comparator<T> reversed() {
            return comparator(Collections.reverseOrder(this));
        }

        /**
         * See <a href="http://download.java.net/jdk8/docs/api/java/util/Comparator.html#thenComparing(java.util.Comparator)">Java 8 doc</a>
         *
         * @since 0.2
         */
        public Comparator<T> thenComparing(final java.util.Comparator<? super T> other) {
            final Comparator<T> me = this;
            return new Comparator<T>() {
                @Override
                public int compare(T o1, T o2) {
                    int ret = (me.compare(o1, o2));
                    return (0 == ret) ? other.compare(o1, o2) : ret;
                }
            };
        }

        /**
         * See <a href="http://download.java.net/jdk8/docs/api/java/util/Comparator.html#thenComparing(java.util.function.Function, java.util.Comparator)">Java 8 doc</a>
         *
         * @since 0.2
         */
        public <U extends Comparable<? super U>> Comparator<T> thenComparing(
                Function<? super T, ? extends U> keyExtractor,
                java.util.Comparator<? super U> keyComparator
        ) {
            return thenComparing(_.F.comparing(keyExtractor, keyComparator));
        }

        /**
         * See <a href="http://download.java.net/jdk8/docs/api/java/util/Comparator.html#thenComparing(java.util.function.Function)">Java 8 doc</a>
         *
         * @since 0.2
         */
        public <U extends Comparable<? super U>> Comparator<T> thenComparing(
                Function<? super T, ? extends U> keyExtractor
        ) {
            return thenComparing(_.F.comparing(keyExtractor));
        }

    }

    /**
     * Adapt a general {@link Func2} function with (T, T, Integer) type into {@link Comparator}
     *
     * @param f   The function takes two params (the same type) and returns integer
     * @param <T> the type of the parameter
     * @return a {@link Comparator} instance backed by function {@code f}
     */
    public static <T> Comparator<T> comparator(final Func2<? super T, ? super T, Integer> f) {
        E.NPE(f);
        if (f instanceof Comparator) {
            return (Comparator<T>) f;
        }
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return f.apply(o1, o2);
            }
        };
    }

    /**
     * Adapt a jdk {@link java.util.Comparator} into {@link Comparator osgl Comparator}
     *
     * @param <T> the element type the comparator compares
     * @param c   the jdk compator
     * @return a {@link Comparator} instance backed by comparator {@code c}
     */
    public static <T> Comparator<T> comparator(final java.util.Comparator<? super T> c) {
        E.NPE(c);
        if (c instanceof Comparator) {
            return (Comparator<T>) c;
        }
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return c.compare(o1, o2);
            }
        };
    }

    /**
     * <code>Predicate</code> is a predefined <code>Function&lt;Boolean, T&gt;</code> typed
     * function with a set of utilities dealing with boolean operations. This is often known
     * as <a href="http://en.wikipedia.org/wiki/Predicate_(mathematical_logic)">Predicate</a>
     * <p/>
     * <p>Note in user application, it should NOT assume an argument is of <code>Predicate</code>
     * typed function, instead the argument should always be declared as <code>Function&lt;Boolean T&gt;</code>:</p>
     * <p/>
     * <pre>
     *     // bad way
     *     void foo(Predicate&lt;MyData&gt; predicate) {
     *         ...
     *     }
     *     // good way
     *     void foo(Function&lt;Boolean, MyData&gt; predicate) {
     *         Predicate&lt;MyData&gt; p = _.predicate(predicate);
     *         ...
     *     }
     * </pre>
     *
     * @since 0.2
     */
    public static abstract class Predicate<T> extends _.F1<T, Boolean> {
        @Override
        public final Boolean apply(T t) {
            return test(t);
        }

        /**
         * Sub class to implement this method to test on the supplied elements
         */
        public abstract boolean test(T t);

        /**
         * Returns a negate function of this
         *
         * @return the negate function
         */
        public Predicate<T> negate() {
            return _.F.negate(this);
        }

        /**
         * Return an <code>Predicate</code> predicate from a list of <code>Function&lt;Boolean, T&gt;</code>
         * with AND operation. For any <code>T t</code> to be tested, if any specified predicates
         * must returns <code>false</code> on it, the resulting predicate will return <code>false</code>.
         *
         * @since 0.2
         */
        public Predicate<T> and(final Function<? super T, Boolean>... predicates) {
            final Predicate<T> me = this;
            return new Predicate<T>() {
                @Override
                public boolean test(T t) {
                    if (!me.test(t)) {
                        return false;
                    }
                    for (Function<? super T, Boolean> f : predicates) {
                        if (!f.apply(t)) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        }

        /**
         * Return an <code>Predicate</code> predicate from a list of <code>Function&lt;Boolean, T&gt;</code>
         * with OR operation. For any <code>T t</code> to be tested, if any specified predicates
         * must returns <code>true</code> on it, the resulting predicate will return <code>true</code>.
         *
         * @since 0.2
         */
        public Predicate<T> or(final Function<? super T, Boolean>... predicates) {
            final Predicate<T> me = this;
            return new Predicate<T>() {
                @Override
                public boolean test(T t) {
                    if (me.test(t)) {
                        return true;
                    }
                    for (Function<? super T, Boolean> f : predicates) {
                        if (f.apply(t)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }

        public <R> F1<T, Option<R>> ifThen(final Function<? super T, R> func) {
            return new F1<T, Option<R>>() {
                @Override
                public Option<R> apply(T t) throws NotAppliedException, Break {
                    if (test(t)) {
                        return some(func.apply(t));
                    }
                    return none();
                }
            };
        }

        public <R> F1<T, Option<R>> elseThen(final Function<? super T, R> func) {
            return negate().ifThen(func);
        }
    }

    /**
     * Convert a <code>Function&lt;T, Boolean&gt;</code> typed function to
     * {@link org.osgl._.Predicate Predicate&lt;T&gt;} function.
     * <p/>
     * <p>If the function specified is already a {@link Predicate}, then
     * the function itself is returned</p>
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> predicate(final Function<? super T, Boolean> f1) {
        if (f1 instanceof Predicate) {
            return (Predicate<T>) f1;
        }
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return f1.apply(t);
            }
        };
    }

    /**
     * Convert a general <code>Function&lt;T, ?&gt;</code> typed function to
     * {@link org.osgl._.Predicate Predicate&lt;T&gt;} function. When the predicate function
     * returned apply to a param, it will first apply the specified {@code f1} to the
     * param, and they call {@link #bool(java.lang.Object)} to evaluate the boolean
     * value of the return object of the application.
     * <p/>
     * <p>If the function specified is already a {@link Predicate}, then
     * the function itself is returned</p>
     *
     * @since 0.2
     */
    public static <T> Predicate<T> generalPredicate(final Function<? super T, ?> f1) {
        if (f1 instanceof Predicate) {
            return (Predicate<T>) f1;
        }
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return bool(f1.apply(t));
            }
        };
    }

    /**
     * Define a visitor (known as Consumer in java 8) function which applied to one parameter and without return type
     *
     * @param <T> the type of the parameter the visitor function applied to
     */
    public abstract static class Visitor<T> extends _.F1<T, Void> {

        /**
         * Construct a visitor without payload
         */
        public Visitor() {
        }

        @Override
        public final Void apply(T t) {
            visit(t);
            return null;
        }

        /**
         * User application to implement visit logic in this method
         *
         * @param t the element been visited
         * @throws Break if the logic decide to break visit progress (usually when visiting a sequence of elements)
         */
        public abstract void visit(T t) throws Break;
    }

    public abstract static class V1<P1> extends Visitor<P1> {
    }

    public abstract static class V2<P1, P2> extends _.F2<P1, P2, Void> {
        @Override
        public final Void apply(P1 p1, P2 p2) throws NotAppliedException, Break {
            visit(p1, p2);
            return null;
        }

        public abstract void visit(P1 p1, P2 p2);
    }

    public abstract static class V3<P1, P2, P3> extends _.F3<P1, P2, P3, Void> {
        @Override
        public final Void apply(P1 p1, P2 p2, P3 p3) throws NotAppliedException, Break {
            visit(p1, p2, p3);
            return null;
        }

        public abstract void visit(P1 p1, P2 p2, P3 p3);
    }

    public abstract static class V4<P1, P2, P3, P4> extends _.F4<P1, P2, P3, P4, Void> {
        @Override
        public final Void apply(P1 p1, P2 p2, P3 p3, P4 p4) throws NotAppliedException, Break {
            visit(p1, p2, p3, p4);
            return null;
        }

        public abstract void visit(P1 p1, P2 p2, P3 p3, P4 p4);
    }

    public abstract static class V5<P1, P2, P3, P4, P5> extends _.F5<P1, P2, P3, P4, P5, Void> {
        @Override
        public final Void apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) throws NotAppliedException, Break {
            visit(p1, p2, p3, p4, p5);
            return null;
        }

        public abstract void visit(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5);
    }

    /**
     * Convert a {@code Function&lt;? super T, Void&gt;} function into a {@link Visitor}
     *
     * @since 0.2
     */
    @SuppressWarnings("unchecked")
    public static <T> Visitor<T> visitor(final Function<? super T, ?> f) {
        if (f instanceof Visitor) {
            return (Visitor<T>) f;
        }
        return new Visitor<T>() {
            @Override
            public void visit(T t) throws Break {
                f.apply(t);
            }
        };
    }

    /**
     * Convert a general {@link Function} to {@link Visitor}, the return value of the function when applied
     * to the param is ignored
     *
     * @param f   A {@link Function} function which return value is ignored
     * @param <T> the type of the param the visitor function visit
     * @return A {@link Visitor} type function
     * @since 0.2
     */
    public static <T> Visitor<T> generalVisitor(final Function<? super T, ?> f) {
        if (f instanceof Visitor) {
            return (Visitor<T>) f;
        }
        return new Visitor<T>() {
            @Override
            public void visit(T t) throws Break {
                f.apply(t);
            }
        };
    }

    /**
     * Return a composed visitor function that only applies when the guard predicate test returns <code>true</code>
     *
     * @param predicate   the predicate to test the element been visited
     * @param visitor the function that visit(accept) the element if the guard tested the element successfully
     * @param <T>     the type of the element be tested and visited
     * @return the composed function
     */
    public static <T> Visitor<T>
    guardedVisitor(final Function<? super T, Boolean> predicate, final Function<? super T, ?> visitor) {
        return new Visitor<T>() {
            @Override
            public void visit(T t) throws Break {
                if (predicate.apply(t)) {
                    visitor.apply(t);
                }
            }
        };
    }

    /**
     * A Break is used to shortcut a sequence of function executions
     */
    public static class Break extends FastRuntimeException {
        private Object payload;

        /**
         * construct a Break without payload
         */
        public Break() {
        }

        /**
         * Construct a Break with payload specified. Note here we can't use generic type for
         * the payload as java does not support generic typed throwable
         *
         * @param payload
         */
        public Break(Object payload) {
            this.payload = payload;
        }

        /**
         * Return the payload
         *
         * @param <T> the type of the return value
         * @return the payload
         */
        public <T> T get() {
            return (T) payload;
        }
    }


    /**
     * A utility method to throw out a Break with payload
     *
     * @param e   the payload object
     * @param <T> the type of the payload
     * @return a Break instance with the payload specified
     */
    public static <T> Break breakOut(T e) {
        throw new Break(e);
    }

    /**
     * A predefined Break instance without payload
     */
    public static final Break BREAK = new Break();

    public static abstract class IndexedVisitor<K, T> extends _.F2<K, T, Void> {

        protected Map<String, ?> _attr = new HashMap<String, Object>();
        protected T _t;
        protected String _s;
        protected boolean _b;
        protected int _i;
        protected float _f;
        protected double _d;
        protected byte _by;
        protected char _c;
        protected long _l;

        public IndexedVisitor() {
        }

        public IndexedVisitor(Map<String, ?> map) {
            _attr = new HashMap<String, Object>(map);
        }

        public IndexedVisitor(T t) {
            _t = t;
        }

        public IndexedVisitor(String s) {
            _s = s;
        }

        public IndexedVisitor(boolean b) {
            _b = b;
        }

        public IndexedVisitor(int i) {
            _i = i;
        }

        public IndexedVisitor(long l) {
            _l = l;
        }

        public IndexedVisitor(double d) {
            _d = d;
        }

        public IndexedVisitor(float f) {
            _f = f;
        }

        public IndexedVisitor(byte b) {
            _by = b;
        }

        public IndexedVisitor(char c) {
            _c = c;
        }

        public T get() {
            return _t;
        }

        public String getStr() {
            return _s;
        }

        public boolean getBoolea() {
            return _b;
        }

        public int getInt() {
            return _i;
        }

        public long getLong() {
            return _l;
        }

        public float getFloat() {
            return _f;
        }

        public double getDouble() {
            return _d;
        }

        public char getChar() {
            return _c;
        }

        public byte getByte() {
            return _by;
        }

        public <E> E get(String key) {
            return (E) _attr.get(key);
        }

        @Override
        public final Void apply(K id, T t) {
            visit(id, t);
            return null;
        }

        public abstract void visit(K id, T t);
    }

    public static <K, T> IndexedVisitor<K, T>
    indexGuardedVisitor(final Function<? super K, Boolean> guard,
                        final Visitor<? super T> visitor
    ) {
        return new IndexedVisitor<K, T>() {
            @Override
            public void visit(K id, T t) throws Break {
                if (guard.apply(id)) {
                    visitor.apply(t);
                }
            }
        };
    }

    /**
     * A Transformer is literally a kind of {@link F1} function
     *
     * @param <FROM> The type of the element the transformer function applied to
     * @param <TO>   The type of the result of transform of <code>&lt;FROM&gt;</code>
     */
    public static abstract class Transformer<TO, FROM> extends _.F1<FROM, TO> {
        @Override
        public final TO apply(FROM from) {
            return transform(from);
        }

        /**
         * The place sub class to implement the transform logic
         */
        public abstract TO transform(FROM from);
    }

    public static abstract class Operator<T> extends _.F1<T, T> {
        @Override
        public abstract Operator<T> inverse();

        @Override
        public abstract Operator<T> times(int n);
    }

    // --- Tuple
    public static class Tuple<A, B> {

        final public A _1;
        final public B _2;

        public Tuple(A _1, B _2) {
            this._1 = _1;
            this._2 = _2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof Tuple) {
                Tuple that = (Tuple) o;
                return eq(that._1, _1) && eq(that._2, _2);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return hc(_1, _2);
        }

        @Override
        public String toString() {
            return "T2(_1: " + _1 + ", _2: " + _2 + ")";
        }
    }

    public static <A, B> Tuple<A, B> Tuple(A a, B b) {
        return new Tuple(a, b);
    }

    public static class T2<A, B> extends Tuple<A, B> {

        public T2(A _1, B _2) {
            super(_1, _2);
        }

        public Map<A, B> asMap() {
            Map<A, B> m = new HashMap<A, B>();
            m.put(_1, _2);
            return m;
        }
    }

    public static <A, B> T2<A, B> T2(A a, B b) {
        return new T2(a, b);
    }

    public static class T3<A, B, C> {

        final public A _1;
        final public B _2;
        final public C _3;

        public T3(A _1, B _2, C _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        public T3<A, B, C> set1(A a) {
            return T3(a, _2, _3);
        }

        public T3<A, B, C> set2(B b) {
            return T3(_1, b, _3);
        }

        public T3<A, B, C> set3(C c) {
            return T3(_1, _2, c);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof T3) {
                T3 that = (T3) o;
                return _.eq(that._1, _1) && _.eq(that._2, _2) && _.eq(that._3, _3);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return _.hc(_1, _2, _3);
        }

        @Override
        public String toString() {
            return "T3(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ")";
        }
    }

    public static <A, B, C> T3<A, B, C> T3(A a, B b, C c) {
        return new T3(a, b, c);
    }

    public static class T4<A, B, C, D> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;

        public T4(A _1, B _2, C _3, D _4) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof T4) {
                T4 that = (T4) o;
                return _.eq(that._1, _1) && _.eq(that._2, _2) && _.eq(that._3, _3) && _.eq(that._4, _4);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return _.hc(_1, _2, _3, _4);
        }

        @Override
        public String toString() {
            return "T4(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ")";
        }
    }

    public static <A, B, C, D> T4<A, B, C, D> T4(A a, B b, C c, D d) {
        return new T4<A, B, C, D>(a, b, c, d);
    }

    public static class T5<A, B, C, D, E> {

        final public A _1;
        final public B _2;
        final public C _3;
        final public D _4;
        final public E _5;

        public T5(A _1, B _2, C _3, D _4, E _5) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
            this._4 = _4;
            this._5 = _5;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof T5) {
                T5 that = (T5) o;
                return _.eq(that._1, _1) && _.eq(that._2, _2) && _.eq(that._3, _3) && _.eq(that._4, _4) && _.eq(that._5, _5);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return _.hc(_1, _2, _3, _4, _5);
        }

        @Override
        public String toString() {
            return "T5(_1: " + _1 + ", _2: " + _2 + ", _3:" + _3 + ", _4:" + _4 + ", _5:" + _5 + ")";
        }
    }

    public static <A, B, C, D, E> T5<A, B, C, D, E> T5(A a, B b, C c, D d, E e) {
        return new T5<A, B, C, D, E>(a, b, c, d, e);
    }

    public static abstract class Option<T> implements Iterable<T>, Serializable {

        private Option() {
        }

        /**
         * Returns  {@code true} if this {@code Option} is not {@link #NONE}
         *
         * @return {@code true} if there is a value present, otherwise {@code false}
         * @since 0.2
         */
        public final boolean isDefined() {
            return this != NONE;
        }

        /**
         * Negate of {@link #isDefined()}
         *
         * @since 0.2
         */
        public boolean notDefined() {
            return !isDefined();
        }

        /**
         * If a value is present in this {@code Option}, returns the value,
         * otherwise throws NoSuchElementException.
         *
         * @return the non-null value held by this {@code Option}
         * @throws NoSuchElementException if this {@code Option} is {@link #NONE}
         */
        public abstract T get() throws NoSuchElementException;

        /**
         * If a value is present, and the value matches the given predicate,
         * return an {@code Option} describing the value, otherwise return
         * {@link #NONE}.
         *
         * @param predicate the function to test the value held by this {@code Option}
         * @return an {@code Option} describing the value of this {@code Option} if
         * a value is present and the value matches the given predicate,
         * otherwise {@link #NONE}
         */
        public final Option<T> filter(Function<? super T, Boolean> predicate) {
            E.NPE(predicate);
            if (notDefined()) {
                return none();
            }
            T v = get();
            if (predicate.apply(v)) {
                return this;
            } else {
                return none();
            }
        }

        /**
         * If a value is present, apply the provided mapping function to it,
         * and if the result is non-null, return an {@code Option} describing
         * the result. Otherwise return {@link #NONE}.
         *
         * @param mapper a mapping function to apply to the value, if present
         * @param <B>    The type of the result of the mapping function
         * @return an Optional describing the result of applying a mapping
         * function to the value of this {@code Option}, if a value is
         * present, otherwise {@link #NONE}
         * @throws NullPointerException if the mapper function is {@code null}
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public final <B> Option<B> map(final Function<? super T, ? extends B> mapper) {
            return isDefined() ? of(mapper.apply(get())) : NONE;
        }

        /**
         * If a value is present, apply the provided {@code Option}-bearing
         * mapping function to it, return that result, otherwise return
         * {@link #NONE}. This method is similar to {@link #map(org.osgl._.Function)},
         * but the provided mapper is one whose result is already an
         * {@code Option}, and if invoked, {@code flatMap} does not wrap it
         * with an additional {@code Option}.
         *
         * @param <B>    The type parameter to the {@code Option} returned by
         * @param mapper a mapping function to apply to the value,
         * @return the result of applying an {@code Option}-bearing mapping
         * function to the value of this {@code Option}, if a value
         * is present, otherwise {@link #NONE}
         * @throws NullPointerException if the mapping function is {@code null}
         *                              or returns a {@code null} result
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public final <B> Option<B> flatMap(final Function<? super T, Option<B>> mapper) {
            E.NPE(mapper);
            Option<B> result = isDefined() ? mapper.apply(get()) : NONE;
            E.NPE(null == result);
            return result;
        }

        /**
         * Return the value if present, otherwise return {@code other}.
         *
         * @param other the value to be returned if there is no value present,
         *              may be {@code null}
         * @return the value, if present, otherwise {@code other}
         */
        public final T orElse(T other) {
            return isDefined() ? get() : other;
        }

        /**
         * Return the value if present, otherwise invoke {@code other} and return
         * the result of that invocation.
         *
         * @param other the function that is applied when no value is presented
         * @return the value if present otherwise the result of {@code other.apply()}
         * @throws NullPointerException if value is not present and other is null
         * @since 0.2
         */
        public final T orElse(Func0<? extends T> other) {
            return isDefined() ? get() : other.apply();
        }

        public final void runWith(Function<? super T, ?> consumer) {
            if (isDefined()) {
                consumer.apply(get());
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Option) {
                Option that = (Option) obj;
                return eq(get(), that.get());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return isDefined() ? get().hashCode() : 0;
        }

        @Override
        public abstract String toString();

        @SuppressWarnings("unchecked")
        public static <T> None<T> none() {
            return (None<T>) NONE;
        }

        /**
         * Returns an {@code Option} with the specified present non-null value.
         *
         * @param value the value that cannot be {@code null}
         * @param <T>   the type of the value
         * @return an Option instance describing the value
         * @throws NullPointerException if the value specified is {@code null}
         * @since 0.2
         */
        public static <T> Some<T> some(T value) {
            E.NPE(value);
            return new Some<T>(value);
        }

        /**
         * Returns an {@code Option} with the specified present value if it is not
         * {@code null} or {@link #NONE} otherwise.
         *
         * @param value the value
         * @param <T>   the type of the value
         * @return an {@code Option} describing the value if it is not {@code null}
         * or {@link #NONE} if the value is {@code null}
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T> Option<T> of(T value) {
            return null == value ? NONE : some(value);
        }

        /**
         * The runtime/instance function namespace
         */
        public final class f {
            private f() {
            }

            /**
             * A function that when applied, returns if the {@code Option} is defined
             */
            public final F0<Boolean> IS_DEFINED = new F0<Boolean>() {
                @Override
                public Boolean apply() {
                    return Option.this != NONE;
                }
            };

            /**
             * Negate of {@link #IS_DEFINED}
             */
            public final F0<Boolean> NOT_DEFINED = _.F.negate(IS_DEFINED);

            /**
             * A function that when applied, returns the value described by this {@code Option}
             */
            public final F0<T> GET = new F0<T>() {
                @Override
                public T apply() throws NotAppliedException, Break {
                    return Option.this.get();
                }
            };

            /**
             * Returns a function that when applied, run {@link _.Option#filter(org.osgl._.Function)} on this
             * {@code Option}
             */
            public final F0<Option<T>> filter(final Function<? super T, Boolean> predicate) {
                return new F0<Option<T>>() {
                    @Override
                    public Option<T> apply() throws NotAppliedException, Break {
                        return Option.this.filter(predicate);
                    }
                };
            }

            /**
             * Returns a function that when applied, run {@link _.Option#map(org.osgl._.Function)} on this
             * {@code Option}
             */
            public final <B> F0<Option<B>> map(final Function<? super T, ? extends B> mapper) {
                return new F0<Option<B>>() {
                    @Override
                    public Option<B> apply() throws NotAppliedException, Break {
                        return Option.this.map(mapper);
                    }
                };
            }

            /**
             * Returns a function that when applied, run {@link _.Option#flatMap(org.osgl._.Function)} on this
             * {@code Option}
             */
            public final <B> F0<Option<B>> flatMap(final Function<? super T, Option<B>> mapper) {
                return new F0<Option<B>>() {
                    @Override
                    public Option<B> apply() throws NotAppliedException, Break {
                        return Option.this.flatMap(mapper);
                    }
                };
            }

            /**
             * Returns a function that when applied, run {@link _.Option#orElse(Object)} on this
             * {@code Option}
             */
            public final <B> F0<T> orElse(final T other) {
                return new F0<T>() {
                    @Override
                    public T apply() throws NotAppliedException, Break {
                        return Option.this.orElse(other);
                    }
                };
            }

            /**
             * Returns a function that when applied, run {@link _.Option#orElse(org.osgl._.Func0)}
             * on this {@code Option}
             */
            public final <B> F0<T> orElse(final Func0<? extends T> other) {
                return new F0<T>() {
                    @Override
                    public T apply() throws NotAppliedException, Break {
                        return Option.this.orElse(other);
                    }
                };
            }

            /**
             * Returns a function that when applied, run {@link _.Option#runWith(org.osgl._.Function)}
             * on this {@code Option}
             */
            public final F0<Void> runWith(final Function<? super T, ?> consumer) {
                return new F0<Void>() {
                    @Override
                    public Void apply() throws NotAppliedException, Break {
                        Option.this.runWith(consumer);
                        return null;
                    }
                };
            }
        }

        public final f f = new f();
    }

    public static class None<T> extends Option<T> {

        private static final long serialVersionUID = 962498820763181262L;

        private None() {
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }

        public static final None INSTANCE = new None();


        @Override
        public T get() {
            throw new NoSuchElementException();
        }

        public Iterator<T> iterator() {
            return Collections.<T>emptyList().iterator();
        }

        @Override
        public String toString() {
            return "NONE";
        }

        private Object readResolve() throws ObjectStreamException {
            return NONE;
        }
    }

    public static class Some<T> extends Option<T> {

        private static final long serialVersionUID = 962498820763181265L;

        final T value;

        public Some(T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.singletonList(value).iterator();
        }

        @Override
        public String toString() {
            return "Some(" + value + ")";
        }
    }

    public static final None NONE = None.INSTANCE;

    @SuppressWarnings("unchecked")
    public static <T> Option<T> some(T a) {
        return null == a ? NONE : new Some(a);
    }

    @SuppressWarnings("unchecked")
    public static <T> None<T> none() {
        return (None<T>) NONE;
    }

    private static class VarListIterator<T> implements ListIterator<T> {
        private Var<T> var;
        private volatile boolean consumed;

        VarListIterator(Var<T> var, boolean consumed) {
            this(var);
            this.consumed = consumed;
        }

        VarListIterator(Var<T> var) {
            this.var = var;
        }

        @Override
        public boolean hasNext() {
            return !consumed;
        }

        @Override
        public boolean hasPrevious() {
            return consumed;
        }

        @Override
        public T next() {
            if (consumed) {
                throw new NoSuchElementException();
            }
            consumed = true;
            return var.get();
        }

        @Override
        public T previous() {
            if (!consumed) {
                throw new NoSuchElementException();
            }
            consumed = false;
            return var.get();
        }

        @Override
        public int nextIndex() {
            return consumed ? 1 : 0;
        }

        @Override
        public int previousIndex() {
            return consumed ? 0 : -1;
        }

        @Override
        public void set(T t) {
            var.set(t);
        }

        @Override
        public void add(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class Var<T> extends ListBase<T> implements C.ListOrSet<T> {

        private T v;

        public Var(T value) {
            v = value;
        }

        @Override
        protected EnumSet<C.Feature> initFeatures() {
            return EnumSet.allOf(C.Feature.class);
        }

        @Override
        public Var<T> accept(Function<? super T, ?> visitor) {
            visitor.apply(v);
            return this;
        }

        @Override
        public T head() throws NoSuchElementException {
            return v;
        }

        @Override
        public Var<T> acceptLeft(Function<? super T, ?> visitor) {
            visitor.apply(v);
            return this;
        }

        @Override
        public <R> R reduceLeft(R identity, Func2<R, T, R> accumulator) {
            return accumulator.apply(identity, v);
        }

        @Override
        public Option<T> reduceLeft(Func2<T, T, T> accumulator) {
            return _.some(v);
        }

        @Override
        public Option<T> findFirst(Function<? super T, Boolean> predicate) {
            if (predicate.apply(v)) {
                return _.some(v);
            } else {
                return _.none();
            }
        }

        @Override
        public C.List<T> take(int n) {
            if (n == 0) {
                return C.list();
            } else if (n < 0) {
                return drop(size() + n);
            } else {
                return this;
            }
        }

        @Override
        public C.List<T> tail() throws UnsupportedOperationException {
            return C.list();
        }

        @Override
        public C.List<T> takeWhile(Function<? super T, Boolean> predicate) {
            if (predicate.apply(v)) {
                return this;
            }
            return C.list();
        }

        @Override
        public C.List<T> drop(int n) throws IllegalArgumentException {
            if (n == 0) {
                return this;
            }
            return C.list();
        }

        @Override
        public C.List<T> dropWhile(Function<? super T, Boolean> predicate) {
            if (predicate.apply(v)) {
                return C.list();
            }
            return this;
        }

        @Override
        public C.ListOrSet<T> filter(Function<? super T, Boolean> predicate) {
            if (predicate.apply(v)) {
                return this;
            }
            return C.empty();
        }

        @Override
        public <R> C.List<R> map(Function<? super T, ? extends R> mapper) {
            return new Var<R>(mapper.apply(v));
        }

        @Override
        public <R> C.List<R> flatMap(Function<? super T, ? extends Iterable<? extends R>> mapper) {
            ListBuilder<R> lb = new ListBuilder<R>(3);
            forEach(_.f1(mapper).andThen(C.F.addAllTo(lb)));
            return lb.toList();
        }

        @Override
        public <T2> C.List<_.T2<T, T2>> zip(Iterable<T2> iterable) {
            Iterator<T2> itr = iterable.iterator();
            if (itr.hasNext()) {
                return new Var<_.T2<T, T2>>(_.T2(v, itr.next()));
            }
            return C.list();
        }

        @Override
        public C.Sequence<T2<T, Integer>> zipWithIndex() {
            return new Var<T2<T, Integer>>(T2(v, 0));
        }

        @Override
        public Var<T> lazy() {
            return this;
        }

        @Override
        public Var<T> eager() {
            return this;
        }

        @Override
        public Var<T> parallel() {
            return this;
        }

        @Override
        public Var<T> sequential() {
            return this;
        }

        @Override
        public int hashCode() {
            return hc(v);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public <R> R reduce(R identity, Func2<R, T, R> accumulator) {
            return reduceLeft(identity, accumulator);
        }

        @Override
        public Option<T> reduce(Func2<T, T, T> accumulator) {
            return reduceLeft(accumulator);
        }

        @Override
        public Option<T> findOne(Function<? super T, Boolean> predicate) {
            if (predicate.apply(v)) {
                return _.some(v);
            } else {
                return _.none();
            }
        }

        @Override
        public boolean anyMatch(Function<? super T, Boolean> predicate) {
            return predicate.apply(v);
        }

        @Override
        public boolean noneMatch(Function<? super T, Boolean> predicate) {
            return !anyMatch(predicate);
        }

        @Override
        public boolean allMatch(Function<? super T, Boolean> predicate) {
            return anyMatch(predicate);
        }

        @Override
        public int size() throws UnsupportedOperationException {
            return 1;
        }

        @Override
        public C.List<T> subList(int fromIndex, int toIndex) {
            if (fromIndex < 0 || toIndex > 1 || fromIndex > toIndex) {
                throw new IndexOutOfBoundsException();
            }
            if (fromIndex == toIndex) {
                return C.list();
            }
            return this;
        }

        @Override
        public boolean addAll(Iterable<? extends T> iterable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public C.List<T> tail(int n) {
            if (n == 0) {
                return C.list();
            }
            return this;
        }

        @Override
        public C.List<T> remove(Function<? super T, Boolean> predicate) {
            throw new UnsupportedOperationException();
        }

        private class Csr implements Cursor<T> {
            private int id = 0;
            @Override
            public boolean isDefined() {
                return 0 == id;
            }

            @Override
            public int index() {
                return id;
            }

            @Override
            public boolean hasNext() {
                return -1 == id;
            }

            @Override
            public boolean hasPrevious() {
                return 1 == id;
            }

            @Override
            public Cursor<T> forward() throws UnsupportedOperationException {
                if (id == -1) {
                    id = 0;
                    return this;
                }
                throw new UnsupportedOperationException();
            }

            @Override
            public Cursor<T> backward() throws UnsupportedOperationException {
                if (id == 1) {
                    id = 0;
                }
                throw new UnsupportedOperationException();
            }

            @Override
            public Cursor<T> parkLeft() {
                id = -1;
                return this;
            }

            @Override
            public Cursor<T> parkRight() {
                id = 1;
                return this;
            }

            @Override
            public T get() throws NoSuchElementException {
                if (id == 0) {
                    return v;
                }
                throw new NoSuchElementException();
            }

            @Override
            public Cursor<T> set(T t) throws IndexOutOfBoundsException, NullPointerException {
                if (id == 0) {
                    v = t;
                    return this;
                }
                throw new IndexOutOfBoundsException();
            }

            @Override
            public Cursor<T> drop() throws NoSuchElementException, UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Cursor<T> prepend(T t) throws IndexOutOfBoundsException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Cursor<T> append(T t) {
                throw new UnsupportedOperationException();
            }
        }

        @Override
        public Cursor<T> locateFirst(Function<T, Boolean> predicate) {
            if (predicate.apply(v)) {
                return new Csr();
            }
            return new Csr().parkRight();
        }

        @Override
        public Cursor<T> locate(Function<T, Boolean> predicate) {
            return locateFirst(predicate);
        }

        @Override
        public Cursor<T> locateLast(Function<T, Boolean> predicate) {
            if (predicate.apply(v)) {
                return new Csr();
            }
            return new Csr().parkLeft();
        }

        @Override
        public C.List<T> insert(int index, T t) throws IndexOutOfBoundsException {
            if (index == 0) {
                return C.list(t, v);
            } else if (index == 1) {
                return C.list(v, t);
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public C.List<T> reverse() {
            return this;
        }

        @Override
        public C.List<T> without(Collection<? super T> col) {
            if (col.contains(v)) {
                return C.list();
            }
            return this;
        }

        @Override
        public C.List<T> acceptRight(Function<? super T, ?> visitor) {
            return accept(visitor);
        }

        @Override
        public <B> C.List<T2<T, B>> zip(List<B> list) {
            if (list.size() == 0) {
                return C.list();
            }
            return C.list(T2(v, list.get(0)));
        }

        @Override
        public boolean add(T t) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            if (index == 0) {
                return new VarListIterator<T>(this);
            } else if (index == 1) {
                return new VarListIterator<T>(this, true);
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public ListIterator<T> listIterator() {
            return new VarListIterator<T>(this);
        }



        @Override
        public T last() throws NoSuchElementException {
            return v;
        }

        @Override
        public Iterator<T> reverseIterator() {
            return listIterator(0);
        }

        @Override
        public T get(int index) {
            if (index == 0) {
                return v;
            }
            throw new IndexOutOfBoundsException();
        }



        public T get() {
            return v;
        }

        public Var<T> set(T value) {
            v = value;
            return this;
        }

        @Override
        public T set(int index, T element) {
            if (0 == index) {
                v = element;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public void add(int index, T element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o) {
            if (_.eq(o, v)) {
                return 0;
            }
            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            return indexOf(o);
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object o) {
            return _.eq(v, o);
        }

        @Override
        public Object[] toArray() {
            return new Object[]{v};
        }

        @Override
        public <T> T[] toArray(T[] a) {
            T[] ta = newArray(a, 1);
            ta[0] = (T)v;
            return ta;
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object o : c) {
                if (_.ne(o, v)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <R> R reduceRight(R identity, Func2<R, T, R> accumulator) {
            return reduce(identity, accumulator);
        }

        @Override
        public Option<T> reduceRight(Func2<T, T, T> accumulator) {
            return reduce(accumulator);
        }

        @Override
        public Option<T> findLast(Function<? super T, Boolean> predicate) {
            return findFirst(predicate);
        }

        @Override
        public Iterator<T> iterator() {
            return listIterator();
        }

        public Var<T> update(Function<T, T> changer) {
            v = changer.apply(v);
            return this;
        }

        public Var<T> update(_.Func0<T> changer) {
            v = changer.apply();
            return this;
        }

        public Option<T> toOption() {
            if (null == v) {
                return _.none();
            } else {
                return _.some(v);
            }
        }

        public class _f {

            public F1<T, Var<T>> setter() {
                return new F1<T, Var<T>>() {
                    @Override
                    public Var<T> apply(T t) throws NotAppliedException, Break {
                        return Var.this.set(t);
                    }
                };
            }

            public <R> F1<R, Var<T>> mapper(final Function<R, T> mapper) {
                return new F1<R, Var<T>>() {
                    @Override
                    public Var<T> apply(R r) throws NotAppliedException, Break {
                        return Var.this.set(mapper.apply(r));
                    }
                };
            }

            public F1<T, Var<T>> updater(final Function<T, T> changer) {
                return new F1<T, Var<T>>() {
                    @Override
                    public Var<T> apply(T t) throws NotAppliedException, Break {
                        return Var.this.update(f1(changer).curry(t));
                    }
                };
            }

            public F1<T, Var<T>> updater(final Func2<T, T, T> changer) {
                return new _.F1<T, Var<T>>() {
                    @Override
                    public Var<T> apply(T t) throws NotAppliedException, Break {
                        return Var.this.update(f2(changer).curry(t));
                    }
                };
            }

        }

        public _f f = new _f();

        public static <T> Var<T> of(T t) {
            return new Var<T>(t);
        }
    }

    public static class Val<T> extends Var<T> {
        public Val(T value) {
            super(value);
        }

        @Override
        public Var<T> set(T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public T set(int index, T element) {
            throw new UnsupportedOperationException();
        }

        public static <T> Val<T> of(T t) {
            return new Val<T>(t);
        }
    }

    public static <T> Var<T> var(T t) {
        return Var.of(t);
    }

    public static <T> Val<T> val(T t) {
        return Val.of(t);
    }

    // --- common utilities

    /**
     * Check if two object is equals to each other.
     *
     * @param a
     * @param b
     * @return {@code true} if {@code a} equals to {@code b}
     * @see #ne(Object, Object)
     */
    public static boolean eq(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (null == a || null == b) {
            return false;
        }
        return a.equals(b);
    }

    /**
     * Check if two object is equals to each other.
     *
     * @param a
     * @param b
     * @return {@code false} if {@code a} equals to {@code b}
     * @see #eq(Object, Object)
     */
    public static boolean ne(Object a, Object b) {
        return !eq(a, b);
    }

    /**
     * Evaluate an object's bool value. The rules are:
     * <table>
     * <thead>
     * <tr>
     * <th>case</th><th>bool value</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><td>{@code null}</td><td>{@code false}</td></tr>
     * <tr><td>{@link #NONE}</td><td>{@code false}</td></tr>
     * <tr><td>String</td><td>{@link S#notEmpty(String) S.notEmpty(v)}</td></tr>
     * <tr><td>Collection</td><td>{@link java.util.Collection#isEmpty() !v.isEmpty()}</td></tr>
     * <tr><td>Array</td><td>length of the array > 0</td></tr>
     * <tr><td>Byte</td><td>{@code v != 0}</td></tr>
     * <tr><td>Char</td><td>{@code v != 0}</td></tr>
     * <tr><td>Integer</td><td>{@code v != 0}</td></tr>
     * <tr><td>Long</td><td>{@code v != 0L}</td></tr>
     * <tr><td>Float</td><td>{@code Math.abs(v) > Float.MIN_NORMAL}</td></tr>
     * <tr><td>Double</td><td>{@code Math.abs(v) > Double.MIN_NORMAL}</td></tr>
     * <tr><td>BigInteger</td><td>{@code !BigInteger.ZERO.equals(v)}</td></tr>
     * <tr><td>BigDecimal</td><td>{@code !BigDecimal.ZERO.equals(v)}</td></tr>
     * <tr><td>File</td><td>{@link java.io.File#exists() v.exists()}</td></tr>
     * <tr><td>{@link org.osgl._.Func0}</td><td>{@code bool(v.apply())}</td></tr>
     * <tr><td>Other types</td><td>{@code true}</td></tr>
     * </tbody>
     * </table>
     *
     * @param v the value to be evaluated
     * @return {@code true} if v evaluate to true, {@code false} otherwise
     */
    public static boolean bool(Object v) {
        if (null == v || NONE == v) {
            return false;
        }
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        if (v instanceof String) {
            return S.notEmpty((String) v);
        }
        if (v instanceof Collection) {
            return !((Collection) v).isEmpty();
        }
        if (v.getClass().isArray()) {
            return 0 < Array.getLength(v);
        }
        if (v instanceof Number) {
            if (v instanceof Float) {
                return bool((float) (Float) v);
            }
            if (v instanceof Double) {
                return bool((double) (Double) v);
            }
            if (v instanceof BigInteger) {
                return bool((BigInteger) v);
            }
            if (v instanceof BigDecimal) {
                return bool((BigDecimal) v);
            }
            return bool(((Number) v).intValue());
        }
        if (v instanceof File) {
            return bool((File) v);
        }
        if (v instanceof Func0) {
            return not(((Func0) v).apply());
        }
        for (Function<Object, Boolean> tester : conf.boolTesters) {
            try {
                return !(tester.apply(v));
            } catch (NotAppliedException e) {
                // ignore
            }
        }
        return true;
    }

    public static boolean bool(boolean v) {
        return v;
    }

    /**
     * Do bool evaluation on a String.
     *
     * @param s the string to be evaluated
     * @return {@code true} if s is not empty
     * @see S#empty(String)
     */
    public static boolean bool(String s) {
        return !S.empty(s);
    }

    /**
     * Do bool evaluation on a collection.
     *
     * @param c the collection to be evaluated
     * @return {@code true} if the collection is not empty
     * @see java.util.Collection#isEmpty()
     */
    public static boolean bool(Collection<?> c) {
        return null != c && !c.isEmpty();
    }

    /**
     * Do bool evaluation on a byte value
     *
     * @param v the value to be evaluated
     * @return {@code true} if the value != 0
     */
    public static boolean bool(byte v) {
        return 0 != v;
    }

    /**
     * Do bool evaluation on a char value
     *
     * @param v the value to be evaluated
     * @return {@code true} if the value != 0
     */
    public static boolean bool(char v) {
        return 0 != v;
    }

    /**
     * Do bool evaluation on a int value
     *
     * @param v the value to be evaluated
     * @return {@code true} if the value != 0
     */
    public static boolean bool(int v) {
        return 0 != v;
    }

    /**
     * Do bool evaluation on a long value
     *
     * @param v the value to be evaluated
     * @return {@code true} if the value != 0
     */
    public static boolean bool(long v) {
        return 0L != v;
    }

    /**
     * Do bool evaluation on a float value
     *
     * @param v the value to be evaluated
     * @return {@code true} if {@code Math.abs(v) > Float.MIN_NORMAL}
     */
    public static boolean bool(float v) {
        return Math.abs(v) > Float.MIN_NORMAL;
    }

    /**
     * Do bool evaluation on a double value
     *
     * @param v the value to be evaluated
     * @return {@code true} if {@code Math.abs(v) > Double.MIN_NORMAL}
     */
    public static boolean bool(double v) {
        return Math.abs(v) > Double.MIN_NORMAL;
    }

    /**
     * Do bool evaluation on a BigDecimal value
     *
     * @param v the value to be evaluated
     * @return {@code true} if {@code !BigDecimal.ZERO.equals(v)}
     */
    public static boolean bool(BigDecimal v) {
        return null != v && !BigDecimal.ZERO.equals(v);
    }

    /**
     * Do bool evaluation on a BigInteger value
     *
     * @param v the value to be evaluated
     * @return {@code true} if {@code !BigInteger.ZERO.equals(v)}
     */
    public static boolean bool(BigInteger v) {
        return null != v && !BigInteger.ZERO.equals(v);
    }

    /**
     * Do bool evaluation on a File instance.
     *
     * @param v the file to be evaluated
     * @return {@code true} if {@code v.exists()}
     */
    public static boolean bool(File v) {
        return null != v && v.exists();
    }

    /**
     * Do bool evaluation on an {@link org.osgl._.Func0} instance. This will call
     * the {@link org.osgl._.Func0#apply()} method and continue to
     * do bool evaluation on the return value
     *
     * @param v the function to be evaluated
     * @return {@code bool(v.apply())}
     */
    public static boolean bool(Func0<?> v) {
        return bool(v.apply());
    }

    /**
     * Returns negative of {@link #bool(java.lang.Object)}
     *
     * @param o the object to be evaluated
     * @return {@code !(bool(o))}
     */
    public static boolean not(Object o) {
        return !(bool(o));
    }

    /**
     * Returns negative of a boolean value
     *
     * @param v the value to be evaluated
     * @return {@code !v}
     */
    public static boolean not(boolean v) {
        return !v;
    }

    /**
     * Returns negative of {@link #bool(java.lang.String)}
     *
     * @param s the String to be evaluated
     * @return {@code !(bool(s))}
     * @see #bool(String)
     */
    public static boolean not(String s) {
        return !bool(s);
    }

    /**
     * Returns negative of {@link #bool(java.util.Collection)}
     *
     * @param c the Collection to be evaluated
     * @return {@code !(bool(c))}
     * @see #bool(java.util.Collection)
     */
    public static boolean not(Collection<?> c) {
        return null == c || c.isEmpty();
    }

    /**
     * Returns negative of {@link #bool(byte)}
     *
     * @param v the byte to be evaluated
     * @return {@code !(bool(v))}
     */
    public static boolean not(byte v) {
        return 0 == v;
    }

    /**
     * Returns negative of {@link #bool(char)}
     *
     * @param v the char to be evaluated
     * @return {@code !(bool(v))}
     */
    public static boolean not(char v) {
        return 0 == v;
    }

    /**
     * Returns negative of {@link #bool(int)}
     *
     * @param v the int to be evaluated
     * @return {@code !(bool(v))}
     */
    public static boolean not(int v) {
        return 0 == v;
    }

    /**
     * Returns negative of {@link #bool(long)}
     *
     * @param v the long to be evaluated
     * @return {@code !(bool(v))}
     */
    public static boolean not(long v) {
        return 0L == v;
    }

    /**
     * Returns negative of {@link #bool(float)}
     *
     * @param v the float to be evaluated
     * @return {@code !(bool(v))}
     */
    public static boolean not(float v) {
        return !bool(v);
    }

    /**
     * Returns negative of {@link #bool(double)}
     *
     * @param v the double to be evaluated
     * @return {@code !(bool(v))}
     */
    public static boolean not(double v) {
        return !bool(v);
    }

    /**
     * Returns negative of {@link #bool(BigDecimal)}
     *
     * @param v the value to be evaluated
     * @return {@code !(bool(v))}
     */
    public static boolean not(BigDecimal v) {
        return !bool(v);
    }

    /**
     * Returns negative of {@link #bool(BigInteger)}
     *
     * @param v the value to be evaluated
     * @return {@code !(bool(v))}
     */
    public static boolean not(BigInteger v) {
        return !bool(v);
    }

    /**
     * Returns negative of {@link #bool(File)}
     *
     * @param file the file to be evaluated
     * @return {@code !(bool(file))}
     */
    public static boolean not(File file) {
        return !bool(file);
    }

    /**
     * Returns negative of {@link #bool(org.osgl._.Func0)}
     *
     * @param f the function to be evaluated
     * @return {@code !(bool(f))}
     */
    public static boolean not(Func0<?> f) {
        return !bool(f);
    }

    /**
     * Check if an object is {@code null} or {@link #NONE}
     *
     * @param o the object to test
     * @return {@link true} if {@code o} is {@code null} or {@code _.NONE}
     */
    public static boolean isNull(Object o) {
        return null == o || NONE == o;
    }

    /**
     * Returns String representation of an object instance. Predicate the object specified
     * is {@code null} or {@code _.NONE}, then an empty string is returned
     *
     * @param o
     * @return a String representation of object
     */
    public static String toString(Object o) {
        if (isNull(o)) {
            return "";
        }
        return o.toString();
    }

    /**
     * Alias of {@link org.osgl.util.S#fmt(String, Object...)}
     *
     * @since 0.2
     */
    public static final String fmt(String tmpl, Object... args) {
        return S.fmt(tmpl, args);
    }

    private static final int HC_INIT = 17;
    private static final int HC_FACT = 37;

    public final static int iterableHashCode(Iterable<?> it) {
        int ret = HC_INIT;
        for (Object o : it) {
            ret = ret * HC_FACT + hc(o);
        }
        return ret;
    }

    public final static int hc(boolean o) {
        return HC_INIT * HC_FACT + (o ? 1 : 0);
    }

    public final static int hc(boolean[] oa) {
        int ret = HC_INIT;
        for (boolean b : oa) {
            ret = ret * HC_FACT + hc(oa);
        }
        return ret;
    }

    public final static int hc(short o) {
        return HC_INIT * HC_FACT + o;
    }

    public final static int hc(short[] oa) {
        int ret = HC_INIT;
        for (short b : oa) {
            ret = ret * HC_FACT + hc(oa);
        }
        return ret;
    }

    public final static int hc(byte o) {
        return HC_INIT * HC_FACT + o;
    }

    public final static int hc(byte[] oa) {
        int ret = HC_INIT;
        for (byte b : oa) {
            ret = ret * HC_FACT + hc(oa);
        }
        return ret;
    }

    public final static int hc(char o) {
        return HC_INIT * HC_FACT + o;
    }

    public final static int hc(char[] oa) {
        int ret = HC_INIT;
        for (char b : oa) {
            ret = ret * HC_FACT + hc(oa);
        }
        return ret;
    }

    public final static int hc(int o) {
        return HC_INIT * HC_FACT + o;
    }

    public final static int hc(int[] oa) {
        int ret = HC_INIT;
        for (int b : oa) {
            ret = ret * HC_FACT + hc(oa);
        }
        return ret;
    }

    public final static int hc(float o) {
        return HC_INIT * HC_FACT + Float.floatToIntBits(o);
    }

    public final static int hc(float[] oa) {
        int ret = HC_INIT;
        for (float b : oa) {
            ret = ret * HC_FACT + hc(oa);
        }
        return ret;
    }

    public final static int hc(long o) {
        return HC_INIT * HC_FACT + (int) (o ^ (o >> 32));
    }

    public final static int hc(long[] oa) {
        int ret = HC_INIT;
        for (long b : oa) {
            ret = ret * HC_FACT + hc(oa);
        }
        return ret;
    }

    public final static int hc(double o) {
        return HC_INIT * HC_FACT + hc(Double.doubleToLongBits(o));
    }

    public final static int hc(double[] oa) {
        int ret = HC_INIT;
        for (double b : oa) {
            ret = ret * HC_FACT + hc(oa);
        }
        return ret;
    }

    public final static int hc(Object o) {
        return hc_(o);
    }

    public static final int hc(Object o1, Object o2) {
        int i = 17;
        i = 31 * i + hc_(o1);
        i = 31 * i + hc_(o2);
        return i;
    }

    public static final int hc(Object o1, Object o2, Object o3) {
        int i = 17;
        i = 31 * i + hc_(o1);
        i = 31 * i + hc_(o2);
        i = 31 * i + hc_(o3);
        return i;
    }

    public static final int hc(Object o1, Object o2, Object o3, Object o4) {
        int i = 17;
        i = 31 * i + hc_(o1);
        i = 31 * i + hc_(o2);
        i = 31 * i + hc_(o3);
        i = 31 * i + hc_(o4);
        return i;
    }

    public static final int hc(Object o1, Object o2, Object o3, Object o4, Object o5) {
        int i = 17;
        i = 31 * i + hc_(o1);
        i = 31 * i + hc_(o2);
        i = 31 * i + hc_(o3);
        i = 31 * i + hc_(o4);
        i = 31 * i + hc_(o5);
        return i;
    }

    /**
     * Calculate hashcode of objects specified
     *
     * @param args
     * @return the calculated hash code
     */
    public final static int hc(Object o1, Object o2, Object o3, Object o4, Object o5, Object... args) {
        int i = hc(o1, o2, o3, o4, o5);
        for (Object o : args) {
            i = 31 * i + hc(o);
        }
        return i;
    }

    private static int hc_(Object o) {
        if (null == o) {
            return HC_INIT * HC_FACT;
        }
        if (o.getClass().isArray()) {
            if (o instanceof int[]) {
                return hc((int[])o);
            } else if (o instanceof long[]) {
                return hc((long[])o);
            } else if (o instanceof char[]) {
                return hc((char[]) o);
            } else if (o instanceof byte[]) {
                return hc((byte[]) o);
            } else if (o instanceof double[]) {
                return hc((double[]) o);
            } else if (o instanceof float[]) {
                return hc((float[]) o);
            } else if (o instanceof short[]) {
                return hc((short[]) o);
            } else if (o instanceof boolean[]) {
                return hc((boolean[]) o);
            }
            int len = Array.getLength(o);
            int hc = 17;
            for (int i = 0; i < len; ++i) {
                hc = 31 * hc + hc_(Array.get(o, i));
            }
            return hc;
        } else {
            return o.hashCode();
        }
    }

    /**
     * Return {@link System#currentTimeMillis() current time millis}
     *
     * @since 0.2
     */
    public static long ms() {
        return System.currentTimeMillis();
    }

    /**
     * Return {@link System#nanoTime() current nano time}
     *
     * @since 0.2
     */
    public static long ns() {
        return System.nanoTime();
    }

    /**
     * Return current time stamp in nano seconds. Alias of {@link #ns()}
     *
     * @since 0.2
     */
    public static long ts() {
        return System.nanoTime();
    }

    public static void echo(String msg, Object... args) {
        System.out.println(S.fmt(msg, args));
    }

    public final static <T> T ensureGet(T t1, T def1) {
        if (null != t1) {
            return t1;
        }
        E.invalidArgIf(null == def1);
        return def1;
    }

    public final static <T> T ensureGet(T t1, T def1, T def2) {
        if (null != t1) {
            return t1;
        }
        if (null != def1) {
            return def1;
        }
        E.npeIf(null == def2);
        return def2;
    }

    public final static <T> T ensureGet(T t1, T def1, T def2, T def3) {
        if (null != t1) {
            return t1;
        }
        if (null != def1) {
            return def1;
        }
        if (null != def2) {
            return def2;
        }
        E.npeIf(null == def3);
        return def2;
    }

    public final static <T> T ensureGet(T t1, List<T> defs) {
        if (null != t1) {
            return t1;
        }
        for (T t : defs) {
            if (null != t) {
                return t;
            }
        }
        throw new NullPointerException();
    }

    public final static <T> T times(Function<T, T> func, T initVal, int n) {
        if (n < 0) {
            throw E.invalidArg("the number of times must not be negative");
        }
        if (n == 0) {
            return initVal;
        }
        T retVal = initVal;
        for (int i = 1; i < n; ++i) {
            retVal = func.apply(retVal);
        }
        return retVal;
    }

    public static <T> Meta<T> meta(T t) {
        return new Meta<T>(t);
    }

    // ---
    public static class Meta<T> {
        private T o;

        public Meta(T obj) {
            E.NPE(obj);
            o = obj;
        }

        public boolean is(Class<?> clz) {
            return clz.isAssignableFrom(o.getClass());
        }

        public boolean isNot(Class<?> clz) {
            return !is(clz);
        }

        public boolean kindOf(Object object) {
            return is(object.getClass());
        }

    }

    /**
     * Cast an object to a type. Returns an {@link Option} describing the casted value if
     * it can be casted to the type specified, otherwise returns {@link #NONE}.
     *
     * @param o   the object to be casted
     * @param c   specify the type to be casted to
     * @param <T> the type of the result option value
     * @return an {@code Option} describing the typed value or {@link #NONE}
     * if it cannot be casted
     */
    public static <T> Option<T> cast(Object o, Class<T> c) {
        if (null == o) {
            return Option.none();
        }
        if (c.isAssignableFrom(o.getClass())) {
            return Option.some((T) o);
        }
        return Option.none();
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o) {
        return (T) o;
    }

    /**
     * Set an object field value using reflection.
     *
     * @param fieldName
     * @param obj
     * @param val
     * @param <T>
     * @param <F>
     * @return the object been set
     */
    public static <T, F> T setField(String fieldName, T obj, F val) {
        Class<?> cls = obj.getClass();
        try {
            Field f;
            try {
                f = cls.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                f = cls.getField(fieldName);
            }
            f.setAccessible(true);
            f.set(obj, val);
        } catch (Exception e) {
            E.unexpected(e);
        }
        return obj;
    }

    public static <T> Class<T> classForName(String className) {
        try {
            return (Class<T>) Class.forName(className);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public static <T> Option<Class<T>> safeClassForName(String className) {
        try {
            return _.some((Class<T>) Class.forName(className));
        } catch (Exception e) {
            return _.none();
        }
    }

    public static <T> T newInstance(String className) {
        try {
            Class<T> c = (Class<T>) Class.forName(className);
            return c.newInstance();
        } catch (Exception e) {
            throw new UnexpectedException();
        }
    }

    public static <T> Option<T> safeNewInstance(String className) {
        try {
            Class<T> c = (Class<T>) Class.forName(className);
            return _.some(c.newInstance());
        } catch (Exception e) {
            return _.none();
        }
    }

    private static boolean testConstructor(Constructor c, Object p, int pos) {
        E.invalidArgIf(pos < 0);
        Class[] pts = c.getParameterTypes();
        if (pos < pts.length) {
            Class pt = pts[pos];
            return (pt.isAssignableFrom(p.getClass()));
        } else {
            return false;
        }
    }

    public final static <T, P1> T newInstance(Class<T> c, P1 p1) {
        try {
            Constructor[] ca = c.getConstructors();
            for (Constructor<T> ct : ca) {
                if (testConstructor(ct, p1, 0)) {
                    return ct.newInstance(p1);
                }
            }
            throw E.unexpected("constructor not found");
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    public final static <T, P1, P2> T newInstance(Class<T> c, P1 p1, P2 p2) {
        try {
            Constructor[] ca = c.getConstructors();
            for (Constructor<T> ct : ca) {
                if (!testConstructor(ct, p1, 0)) {
                    continue;
                }
                if (!testConstructor(ct, p2, 1)) {
                    continue;
                }
                return ct.newInstance(p1, p2);
            }
            throw E.unexpected("constructor not found");
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    public final static <T, P1, P2, P3> T newInstance(Class<T> c, P1 p1, P2 p2, P3 p3) {
        try {
            Constructor[] ca = c.getConstructors();
            for (Constructor<T> ct : ca) {
                if (!testConstructor(ct, p1, 0)) {
                    continue;
                }
                if (!testConstructor(ct, p2, 1)) {
                    continue;
                }
                if (!testConstructor(ct, p3, 2)) {
                    continue;
                }
                return ct.newInstance(p1, p2, p3);
            }
            throw E.unexpected("constructor not found");
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    public final static <T, P1, P2, P3, P4> T newInstance(Class<T> c, P1 p1, P2 p2, P3 p3, P4 p4) {
        try {
            Constructor[] ca = c.getConstructors();
            for (Constructor<T> ct : ca) {
                if (!testConstructor(ct, p1, 0)) {
                    continue;
                }
                if (!testConstructor(ct, p2, 1)) {
                    continue;
                }
                if (!testConstructor(ct, p3, 2)) {
                    continue;
                }
                if (!testConstructor(ct, p4, 4)) {
                    continue;
                }
                return ct.newInstance(p1, p2, p3, p4);
            }
            throw E.unexpected("constructor not found");
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    public final static <T, P1, P2, P3, P4, P5> T newInstance(Class<T> c, P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
        try {
            Constructor[] ca = c.getConstructors();
            for (Constructor<T> ct : ca) {
                if (!testConstructor(ct, p1, 0)) {
                    continue;
                }
                if (!testConstructor(ct, p2, 1)) {
                    continue;
                }
                if (!testConstructor(ct, p3, 2)) {
                    continue;
                }
                if (!testConstructor(ct, p4, 4)) {
                    continue;
                }
                if (!testConstructor(ct, p5, 5)) {
                    continue;
                }
                return ct.newInstance(p1, p2, p3, p4, p5);
            }
            throw E.unexpected("constructor not found");
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    public static <T> byte[] serialize(T obj) {
        E.NPE(obj);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public static <T> T materialize(byte[] ba, Class<T> c) {
        E.NPE(ba);
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(ba);
            ObjectInputStream ois = new ObjectInputStream(bais);
            T t = (T) ois.readObject();
            ois.close();
            return t;
        } catch (IOException e) {
            throw E.ioException(e);
        } catch (ClassNotFoundException e) {
            throw E.unexpected(e);
        }
    }

    public static <T> T materialize(byte[] ba) {
        E.NPE(ba);
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(ba);
            ObjectInputStream ois = new ObjectInputStream(bais);
            T t = (T) ois.readObject();
            ois.close();
            return t;
        } catch (IOException e) {
            throw E.ioException(e);
        } catch (ClassNotFoundException e) {
            throw E.unexpected(e);
        }
    }

    /**
     * Create an new array with specified array type and length
     * @param model the model array
     * @param <T> the array component type
     * @return an new array with the same component type of model and length of model
     */
    public static <T> T[] newArray(T[] model) {
        return newArray(model, model.length);
    }

    /**
     * Create an new array with specified type and length
     * @param model the model array
     * @param size the new array length
     * @param <T> the component type
     * @return an new array with the same type of model and length equals to size
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(T[] model, int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        Class<?> c = model.getClass();
        if (c == Object[].class) {
            return (T[]) new Object[size];
        }
        return (T[]) Array.newInstance(c.getComponentType(), size);
    }

    public static int[] newArray(int[] model) {
        return newArray(model, model.length);
    }

    public static int[] newArray(int[] model, int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        return new int[size];
    }


    public static byte[] newArray(byte[] model) {
        return newArray(model, model.length);
    }

    public static byte[] newArray(byte[] model, int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        return new byte[size];
    }

    public static char[] newArray(char[] model) {
        return newArray(model, model.length);
    }

    public static char[] newArray(char[] model, int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        return new char[size];
    }

    public static short[] newArray(short[] model) {
        return newArray(model, model.length);
    }

    public static short[] newArray(short[] model, int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        return new short[size];
    }

    public static long[] newArray(long[] model) {
        return newArray(model, model.length);
    }

    public static long[] newArray(long[] model, int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        return new long[size];
    }

    public static float[] newArray(float[] model) {
        return newArray(model, model.length);
    }

    public static float[] newArray(float[] model, int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        return new float[size];
    }

    public static double[] newArray(double[] model) {
        return newArray(model, model.length);
    }

    public static double[] newArray(double[] model, int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        return new double[size];
    }

    public static boolean[] newArray(boolean[] model) {
        return newArray(model, model.length);
    }

    public static boolean[] newArray(boolean[] model, int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        return new boolean[size];
    }

    public static <T> T[] concat(T[] a1, T[] a2) {
        T[] ret = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, ret, a1.length, a2.length);
        return ret;
    }

    public static <T> T[] concat(T[] a1, T[] a2, T[]... rest) {
        int l1 = a1.length, l2 = a2.length, l12 = l1 + l2, len = l12;
        for (T[] a: rest) {
            len += a.length;
        }
        T[] ret = Arrays.copyOf(a1, len);
        System.arraycopy(a2, 0, ret, l1, l2);
        int offset = l12;
        for (T[] a : rest) {
            int la = a.length;
            System.arraycopy(a, 0, ret, offset, la);
            offset += la;
        }

        return ret;
    }

    public static int[] concat(int[] a1, int[] a2) {
        int[] ret = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, ret, a1.length, a2.length);
        return ret;
    }

    public static int[] concat(int[] a1, int[] a2, int[]... rest) {
        int l1 = a1.length, l2 = a2.length, l12 = l1 + l2, len = l12;
        for (int[] a: rest) {
            len += a.length;
        }
        int[] ret = Arrays.copyOf(a1, len);
        System.arraycopy(a2, 0, ret, l1, l2);
        int offset = l12;
        for (int[] a : rest) {
            int la = a.length;
            System.arraycopy(a, 0, ret, offset, la);
            offset += la;
        }

        return ret;
    }

    public static boolean[] concat(boolean[] a1, boolean[] a2) {
        boolean[] ret = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, ret, a1.length, a2.length);
        return ret;
    }

    public static boolean[] concat(boolean[] a1, boolean[] a2, boolean[]... rest) {
        int l1 = a1.length, l2 = a2.length, l12 = l1 + l2, len = l12;
        for (boolean[] a: rest) {
            len += a.length;
        }
        boolean[] ret = Arrays.copyOf(a1, len);
        System.arraycopy(a2, 0, ret, l1, l2);
        int offset = l12;
        for (boolean[] a : rest) {
            int la = a.length;
            System.arraycopy(a, 0, ret, offset, la);
            offset += la;
        }

        return ret;
    }

    public static byte[] concat(byte[] a1, byte[] a2) {
        byte[] ret = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, ret, a1.length, a2.length);
        return ret;
    }

    public static byte[] concat(byte[] a1, byte[] a2, byte[]... rest) {
        int l1 = a1.length, l2 = a2.length, l12 = l1 + l2, len = l12;
        for (byte[] a: rest) {
            len += a.length;
        }
        byte[] ret = Arrays.copyOf(a1, len);
        System.arraycopy(a2, 0, ret, l1, l2);
        int offset = l12;
        for (byte[] a : rest) {
            int la = a.length;
            System.arraycopy(a, 0, ret, offset, la);
            offset += la;
        }

        return ret;
    }

    public static short[] concat(short[] a1, short[] a2) {
        short[] ret = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, ret, a1.length, a2.length);
        return ret;
    }

    public static short[] concat(short[] a1, short[] a2, short[]... rest) {
        int l1 = a1.length, l2 = a2.length, l12 = l1 + l2, len = l12;
        for (short[] a: rest) {
            len += a.length;
        }
        short[] ret = Arrays.copyOf(a1, len);
        System.arraycopy(a2, 0, ret, l1, l2);
        int offset = l12;
        for (short[] a : rest) {
            int la = a.length;
            System.arraycopy(a, 0, ret, offset, la);
            offset += la;
        }

        return ret;
    }

    public static char[] concat(char[] a1, char[] a2) {
        char[] ret = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, ret, a1.length, a2.length);
        return ret;
    }

    public static char[] concat(char[] a1, char[] a2, char[]... rest) {
        int l1 = a1.length, l2 = a2.length, l12 = l1 + l2, len = l12;
        for (char[] a: rest) {
            len += a.length;
        }
        char[] ret = Arrays.copyOf(a1, len);
        System.arraycopy(a2, 0, ret, l1, l2);
        int offset = l12;
        for (char[] a : rest) {
            int la = a.length;
            System.arraycopy(a, 0, ret, offset, la);
            offset += la;
        }

        return ret;
    }

    public static long[] concat(long[] a1, long[] a2) {
        long[] ret = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, ret, a1.length, a2.length);
        return ret;
    }

    public static long[] concat(long[] a1, long[] a2, long[]... rest) {
        int l1 = a1.length, l2 = a2.length, l12 = l1 + l2, len = l12;
        for (long[] a: rest) {
            len += a.length;
        }
        long[] ret = Arrays.copyOf(a1, len);
        System.arraycopy(a2, 0, ret, l1, l2);
        int offset = l12;
        for (long[] a : rest) {
            int la = a.length;
            System.arraycopy(a, 0, ret, offset, la);
            offset += la;
        }

        return ret;
    }

    public static float[] concat(float[] a1, float[] a2) {
        float[] ret = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, ret, a1.length, a2.length);
        return ret;
    }

    public static float[] concat(float[] a1, float[] a2, float[]... rest) {
        int l1 = a1.length, l2 = a2.length, l12 = l1 + l2, len = l12;
        for (float[] a: rest) {
            len += a.length;
        }
        float[] ret = Arrays.copyOf(a1, len);
        System.arraycopy(a2, 0, ret, l1, l2);
        int offset = l12;
        for (float[] a : rest) {
            int la = a.length;
            System.arraycopy(a, 0, ret, offset, la);
            offset += la;
        }

        return ret;
    }

    public static double[] concat(double[] a1, double[] a2) {
        double[] ret = Arrays.copyOf(a1, a1.length + a2.length);
        System.arraycopy(a2, 0, ret, a1.length, a2.length);
        return ret;
    }

    public static double[] concat(double[] a1, double[] a2, double[]... rest) {
        int l1 = a1.length, l2 = a2.length, l12 = l1 + l2, len = l12;
        for (double[] a: rest) {
            len += a.length;
        }
        double[] ret = Arrays.copyOf(a1, len);
        System.arraycopy(a2, 0, ret, l1, l2);
        int offset = l12;
        for (double[] a : rest) {
            int la = a.length;
            System.arraycopy(a, 0, ret, offset, la);
            offset += la;
        }

        return ret;
    }

    public static Byte[] asObject(byte[] pa) {
        int len = pa.length;
        Byte[] oa = new Byte[len];
        for (int i = 0; i < len; ++i) {
            oa[i] = pa[i];
        }
        return oa;
    }

    public static Short[] asObject(short[] pa) {
        int len = pa.length;
        Short[] oa = new Short[len];
        for (int i = 0; i < len; ++i) {
            oa[i] = pa[i];
        }
        return oa;
    }

    public static Integer[] asObject(int[] pa) {
        int len = pa.length;
        Integer[] oa = new Integer[len];
        for (int i = 0; i < len; ++i) {
            oa[i] = pa[i];
        }
        return oa;
    }

    public static Long[] asObject(long[] pa) {
        int len = pa.length;
        Long[] oa = new Long[len];
        for (int i = 0; i < len; ++i) {
            oa[i] = pa[i];
        }
        return oa;
    }

    public static Float[] asObject(float[] pa) {
        int len = pa.length;
        Float[] oa = new Float[len];
        for (int i = 0; i < len; ++i) {
            oa[i] = pa[i];
        }
        return oa;
    }

    public static Double[] asObject(double[] pa) {
        int len = pa.length;
        Double[] oa = new Double[len];
        for (int i = 0; i < len; ++i) {
            oa[i] = pa[i];
        }
        return oa;
    }
    // --- eof common utilities

    /**
     * Namespace to configure or extend OSGL _ utilities
     */
    public static final class Conf {
        private final List<Function<Object, Boolean>> boolTesters = new ArrayList<Function<Object, Boolean>>();

        /**
         * Register a boolean tester. A boolean tester is a {@link org.osgl._.Function Function&ltBoolean, Object&gt} type function
         * that applied to {@code Object} type parameter and returns a boolean value of the Object been tested. It
         * should throw out {@link NotAppliedException} if the type of the object been tested is not recognized.
         * there is no need to test if the parameter is {@code null} in the tester as the utility will garantee the
         * object passed in is not null
         * <p/>
         * <pre>
         *  _.Conf.registerBoolTester(new _.F1&lt;Boolean, Object&gt;() {
         *      @Override
         *      public Boolean apply(Object o) {
         *          if (o instanceof Score) {
         *              return ((Score)o).intValue() > 60;
         *          }
         *          if (o instanceof Person) {
         *              return ((Person)o).age() > 16;
         *          }
         *          ...
         *          // since we do not recognize the object type, raise the NotAppliedException out
         *          throw new org.osgl.E.NotAppliedException();
         *      }
         *  });
         * </pre>
         *
         * @param tester
         */
        public Conf registerBoolTester(Function<Object, Boolean> tester) {
            E.NPE(tester);
            boolTesters.add(tester);
            return this;
        }
    }

    public static final Conf conf = new Conf();

    /**
     * The namespace to aggregate predefined core functions
     */
    public enum F {
        ;


        /**
         * Return a one variable function that throw out a {@link Break} with payload specified when a predicate return
         * <code>true</code> on an element been tested
         *
         * @since 0.2
         */
        public static <P, T> F1<T, Void> breakIf(final Function<? super T, Boolean> predicate, final P payload) {
            return new F1<T, Void>() {
                @Override
                public Void apply(T t) {
                    if (predicate.apply(t)) {
                        throw breakOut(payload);
                    }
                    return null;
                }
            };
        }

        /**
         * Return a one variable function that throw out a {@link Break} when a predicate return
         * <code>true</code> on an element been tested. There is no payload specified and the <code>Break</code>
         * will use test result i.e. <code>true</code> as the payload
         *
         * @since 0.2
         */
        public static <T> F1<T, Void> breakIf(final Function<? super T, Boolean> predicate) {
            return new F1<T, Void>() {
                @Override
                public Void apply(T t) {
                    if (predicate.apply(t)) {
                        throw breakOut(t);
                    }
                    return null;
                }
            };
        }

        /**
         * Return a two variables function that throw out a {@link Break} with payload specified when
         * a two variables predicate return <code>true</code> on an element been tested
         *
         * @since 0.2
         */
        public static <P, T1, T2> F2<T1, T2, Void> breakIf(final Func2<? super T1, ? super T2, Boolean> predicate,
                                                           final P payload
        ) {
            return new F2<T1, T2, Void>() {
                @Override
                public Void apply(T1 t1, T2 t2) {
                    if (predicate.apply(t1, t2)) {
                        throw breakOut(payload);
                    }
                    return null;
                }
            };
        }

        /**
         * Return a two variables function that throw out a {@link Break} when a two variables predicate return
         * <code>true</code> on an element been tested. There is no payload specified and the <code>Break</code>
         * will use test result i.e. <code>true</code> as the payload
         *
         * @since 0.2
         */
        public static <T1, T2> F2<T1, T2, Void> breakIf(final Func2<? super T1, ? super T2, Boolean> predicate) {
            return new F2<T1, T2, Void>() {
                @Override
                public Void apply(T1 t1, T2 t2) {
                    if (predicate.apply(t1, t2)) {
                        throw breakOut(true);
                    }
                    return null;
                }
            };
        }

        /**
         * Return a three variables function that throw out a {@link Break} with payload specified when
         * a three variables predicate return <code>true</code> on an element been tested
         *
         * @since 0.2
         */
        public static <P, T1, T2, T3> F3<T1, T2, T3, Void> breakIf(
                final Func3<? super T1, ? super T2, ? super T3, Boolean> predicate,
                final P payload
        ) {
            return new F3<T1, T2, T3, Void>() {
                @Override
                public Void apply(T1 t1, T2 t2, T3 t3) {
                    if (predicate.apply(t1, t2, t3)) {
                        throw breakOut(payload);
                    }
                    return null;
                }
            };
        }

        /**
         * Return a three variables function that throw out a {@link Break} when a three variables predicate return
         * <code>true</code> on an element been tested. There is no payload specified and the <code>Break</code>
         * will use test result i.e. <code>true</code> as the payload
         *
         * @since 0.2
         */
        public static <T1, T2, T3> F3<T1, T2, T3, Void> breakIf(
                final Func3<? super T1, ? super T2, ? super T3, Boolean> predicate
        ) {
            return new F3<T1, T2, T3, Void>() {
                @Override
                public Void apply(T1 t1, T2 t2, T3 t3) {
                    if (predicate.apply(t1, t2, t3)) {
                        throw breakOut(true);
                    }
                    return null;
                }
            };
        }

        /**
         * Return a four variables function that throw out a {@link Break} with payload specified when
         * a four variables predicate return <code>true</code> on an element been tested
         *
         * @since 0.2
         */
        public static <P, T1, T2, T3, T4> F4<T1, T2, T3, T4, Void> breakIf(
                final Func4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> predicate, final P payload
        ) {
            return new F4<T1, T2, T3, T4, Void>() {
                @Override
                public Void apply(T1 t1, T2 t2, T3 t3, T4 t4) {
                    if (predicate.apply(t1, t2, t3, t4)) {
                        throw breakOut(payload);
                    }
                    return null;
                }
            };
        }

        /**
         * Return a four variables function that throw out a {@link Break} when a four variables predicate return
         * <code>true</code> on an element been tested. There is no payload specified and the <code>Break</code>
         * will use test result i.e. <code>true</code> as the payload
         *
         * @since 0.2
         */
        public static <T1, T2, T3, T4> F4<T1, T2, T3, T4, Void> breakIf(
                final Func4<? super T1, ? super T2, ? super T3, ? super T4, Boolean> predicate
        ) {
            return new F4<T1, T2, T3, T4, Void>() {
                @Override
                public Void apply(T1 t1, T2 t2, T3 t3, T4 t4) {
                    if (predicate.apply(t1, t2, t3, t4)) {
                        throw breakOut(true);
                    }
                    return null;
                }
            };
        }

        /**
         * Return a five variables function that throw out a {@link Break} with payload specified when
         * a five variables predicate return <code>true</code> on an element been tested
         *
         * @since 0.2
         */
        public static <P, T1, T2, T3, T4, T5> F5<T1, T2, T3, T4, T5, Void> breakIf(
                final Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> predicate,
                final P payload
        ) {
            return new F5<T1, T2, T3, T4, T5, Void>() {
                @Override
                public Void apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
                    if (predicate.apply(t1, t2, t3, t4, t5)) {
                        throw breakOut(payload);
                    }
                    return null;
                }
            };
        }

        /**
         * Return a five variables function that throw out a {@link Break} when a five variables predicate return
         * <code>true</code> on an element been tested. There is no payload specified and the <code>Break</code>
         * will use test result i.e. <code>true</code> as the payload
         *
         * @since 0.2
         */
        public static <T1, T2, T3, T4, T5> F5<T1, T2, T3, T4, T5, Void> breakIf(
                final Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, Boolean> predicate
        ) {
            return new F5<T1, T2, T3, T4, T5, Void>() {
                @Override
                public Void apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
                    if (predicate.apply(t1, t2, t3, t4, t5)) {
                        throw breakOut(true);
                    }
                    return null;
                }
            };
        }

        /**
         * Returns a composed {@link org.osgl._.Predicate} function that for any given parameter, the test result is <code>true</code>
         * only when all of the specified predicates returns <code>true</code> when applied to the parameter
         *
         * @param predicates an iterable of predicates that can be applied to a parameter and returns boolean value
         * @param <T>        the type of the parameter the predicates applied to
         * @return a composed function
         * @since 0.2
         */
        public static <T> Predicate<T> and(final Iterable<Function<? super T, Boolean>> predicates) {
            return new Predicate<T>() {
                @Override
                public boolean test(T t) {
                    for (Function<? super T, Boolean> cond : predicates) {
                        if (!cond.apply(t)) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        }

        /**
         * Returns a composed {@link org.osgl._.Predicate} function that for any given parameter, the test result is <code>true</code>
         * only when all of the specified predicates returns <code>true</code> when applied to the parameter
         *
         * @param predicates an array of predicates that can be applied to a parameter and returns boolean value
         * @param <T>        the type of the parameter the predicates applied to
         * @return a composed function
         * @since 0.2
         */
        public static <T> Predicate<T> and(final Function<? super T, Boolean>... predicates) {
            //TODO return and(C1.list(predicates));
            return null;
        }

        /**
         * Returns a composed {@link org.osgl._.Predicate} function that for any given parameter, the test result is <code>true</code>
         * only when any one of the specified predicates returns <code>true</code> when applied to the parameter
         *
         * @param predicates an iterable of predicates that can be applied to a parameter and returns boolean value
         * @param <T>        the type of the parameter the predicates applied to
         * @return a composed function
         * @since 0.2
         */
        public static <T> Predicate<T> or(final Iterable<Function<? super T, Boolean>> predicates) {
            return new Predicate<T>() {
                @Override
                public boolean test(T t) {
                    for (Function<? super T, Boolean> cond : predicates) {
                        if (cond.apply(t)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }

        /**
         * Returns a composed {@link org.osgl._.Predicate} function that for any given parameter, the test result is <code>true</code>
         * only when any one of the specified predicates returns <code>true</code> when applied to the parameter
         *
         * @param predicates an array of predicates that can be applied to a parameter and returns boolean value
         * @param <T>        the type of the parameter the predicates applied to
         * @return a composed function
         * @since 0.2
         */
        public static <T> Predicate<T> or(final Function<? super T, Boolean>... predicates) {
            // TODO return or(C1.list(predicates));
            return null;
        }

        /**
         * Alias of {@link #or(Iterable)}
         *
         * @since 0.2
         */
        public static <T> Predicate<T> any(final Iterable<Function<? super T, Boolean>> predicates) {
            return or(predicates);
        }

        /**
         * Alias of {@link #or(org.osgl._.Function[])}
         *
         * @since 0.2
         */
        public static <T> Predicate<T> any(final Function<? super T, Boolean>... predicates) {
            return or(predicates);
        }

        /**
         * Negation of {@link #or(Iterable)}
         *
         * @since 0.2
         */
        public static <T> Predicate<T> none(final Iterable<Function<? super T, Boolean>> predicates) {
            return negate(or(predicates));
        }

        /**
         * Negation of {@link #or(org.osgl._.Function[])}
         *
         * @since 0.2
         */
        public static <T> Predicate<T> none(final Function<? super T, Boolean>... predicates) {
            return negate(or(predicates));
        }

        /**
         * Returns a function that evaluate an argument's boolean value and negate the value
         *
         * @param <T> the type of the argument the function applied to
         * @return the function returns true if an argument evaluated to false, or vice versa
         */
        public static <T> F1<T, Boolean> not() {
            return new F1<T, Boolean>() {
                @Override
                public Boolean apply(T t) throws NotAppliedException, Break {
                    return _.not(t);
                }
            };
        }

        public static <X, Y> Bijection<Y, X> inverse(final Bijection<X, Y> f) {
            return _.f1(f.inverse());
        }

        /**
         * Returns a negate function of the specified predicate function
         *
         * @param predicate the specified function that returns boolean value
         * @return the function that negate the specified predicate
         */
        public static F0<Boolean> negate(final Func0<Boolean> predicate) {
            return new F0<Boolean>() {
                @Override
                public Boolean apply() {
                    return !predicate.apply();
                }
            };
        }

        /**
         * Returns a negate function of the specified predicate function that applied to 1 params and
         * returns boolean value
         *
         * @param predicate the specified function that applied to the parameter and returns boolean value
         * @param <T>       the type of the parameter to be applied
         * @return the function that negate the specified predicate
         */
        public static <T> Predicate<T> negate(final Function<? super T, Boolean> predicate) {
            return new Predicate<T>() {
                @Override
                public boolean test(T t) {
                    return !predicate.apply(t);
                }
            };
        }

        /**
         * Returns a negate function of the specified predicate that applied to 2 parameters and returns boolean value
         *
         * @param predicate the function applied to 2 params and return boolean value
         * @param <P1>      type of param one
         * @param <P2>      type of param two
         * @return the function that negate predicate specified
         */
        public static <P1, P2> F2<P1, P2, Boolean> negate(final Func2<? super P1, ? super P2, Boolean> predicate) {
            return new F2<P1, P2, Boolean>() {
                @Override
                public Boolean apply(P1 p1, P2 p2) {
                    return !predicate.apply(p1, p2);
                }
            };
        }

        /**
         * Returns a negate function of the specified predicate that applied to 3 parameters and returns boolean value
         *
         * @param predicate the function applied to 2 params and return boolean value
         * @param <P1>      type of param one
         * @param <P2>      type of param two
         * @param <P3>      type of param three
         * @return the function that negate predicate specified
         */
        public static <P1, P2, P3> F3<P1, P2, P3, Boolean> negate(
                final Func3<? super P1, ? super P2, ? super P3, Boolean> predicate
        ) {
            return new F3<P1, P2, P3, Boolean>() {
                @Override
                public Boolean apply(P1 p1, P2 p2, P3 p3) {
                    return !predicate.apply(p1, p2, p3);
                }
            };
        }

        /**
         * Returns a negate function of the specified predicate that applied to 3 parameters and returns boolean value
         *
         * @param predicate the function applied to 2 params and return boolean value
         * @param <P1>      type of param one
         * @param <P2>      type of param two
         * @param <P3>      type of param three
         * @param <P4>      type of param four
         * @return the function that negate predicate specified
         */
        public static <P1, P2, P3, P4> F4<P1, P2, P3, P4, Boolean> negate(
                final Func4<? super P1, ? super P2, ? super P3, ? super P4, Boolean> predicate
        ) {
            return new F4<P1, P2, P3, P4, Boolean>() {
                @Override
                public Boolean apply(P1 p1, P2 p2, P3 p3, P4 p4) {
                    return !predicate.apply(p1, p2, p3, p4);
                }
            };
        }

        /**
         * Returns a negate function of the specified predicate that applied to 3 parameters and returns boolean value
         *
         * @param predicate the function applied to 2 params and return boolean value
         * @param <P1>      type of param one
         * @param <P2>      type of param two
         * @param <P3>      type of param three
         * @param <P4>      type of param four
         * @param <P5>      type of param five
         * @return the function that negate predicate specified
         */
        public static <P1, P2, P3, P4, P5> F5<P1, P2, P3, P4, P5, Boolean> negate(
                final Func5<? super P1, ? super P2, ? super P3, ? super P4, ? super P5, Boolean> predicate
        ) {
            return new F5<P1, P2, P3, P4, P5, Boolean>() {
                @Override
                public Boolean apply(P1 p1, P2 p2, P3 p3, P4 p4, P5 p5) {
                    return !predicate.apply(p1, p2, p3, p4, p5);
                }
            };
        }

        /**
         * A predefined forever true predicate which returns <code>true</code>
         * on any element been tested
         *
         * @see #yes()
         * @since 0.2
         */
        public static final Predicate TRUE = new Predicate() {
            @Override
            public boolean test(Object o) {
                return true;
            }
        };

        /**
         * A type-safe version of {@link #TRUE}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T> Predicate<T> yes() {
            return TRUE;
        }

        /**
         * A predefined forever FALSE predicate which always return
         * <code>false</code> for whatever element been tested
         *
         * @see #no()
         * @since 0.2
         */
        public static final Predicate FALSE = negate(TRUE);

        /**
         * A type-safe version of {@link #FALSE}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T> Predicate<T> no() {
            return FALSE;
        }

        /**
         * A predefined <code>Predicate</code> predicate test if the
         * element specified is <code>null</code> or {@link _#NONE}.
         *
         * @since 0.2
         */
        public static final Predicate IS_NULL = new Predicate() {
            @Override
            public boolean test(Object o) {
                return null == o || NONE == o;
            }
        };

        /**
         * The type-safe version of {@link #IS_NULL}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T> Predicate<T> isNull() {
            return IS_NULL;
        }

        /**
         * The type-safe version of {@link #IS_NULL}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T> Predicate<T> isNull(Class<T> c) {
            return IS_NULL;
        }

        /**
         * A predefined <code>Predicate</code> predicate test if the element
         * specified is NOT null
         *
         * @since 0.2
         */
        public static final Predicate NOT_NULL = negate(IS_NULL);

        /**
         * The type-safe version of {@link #NOT_NULL}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T> Predicate<T> notNull() {
            return NOT_NULL;
        }

        /**
         * A predefined function that when apply to a parameter it return it directly, so for any Object o
         * <pre>
         *     IDENTITY.apply(o) == o;
         * </pre>
         *
         * @since 0.2
         */
        public static final F1 IDENTITY = new F1() {
            @Override
            public Object apply(Object o) {
                return o;
            }
        };

        /**
         * The type-safe version of {@link #IDENTITY}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T> F1<T, T> identity() {
            return IDENTITY;
        }

        private static <T extends Comparable<T>> F2<T, T, Boolean> _cmp(final boolean lt) {
            return new F2<T, T, Boolean>() {
                @Override
                public Boolean apply(T t1, T t2
                ) throws NotAppliedException, Break {
                    if (lt) {
                        return (t1.compareTo(t2) < 0);
                    } else {
                        return (t1.compareTo(t2) > 0);
                    }
                }
            };
        }

        private static <T extends Comparable<T>> F1<T, Boolean> _cmp(final T v, final boolean lt) {
            return new F1<T, Boolean>() {
                @Override
                public Boolean apply(T t) throws NotAppliedException, Break {
                    if (lt) {
                        return t.compareTo(v) < 0;
                    } else {
                        return t.compareTo(v) > 0;
                    }
                }
            };
        }

        /**
         * Returns a function that apply to one parameter and compare it with the value {@code v} specified,
         * returns {@code true} if the parameter applied is less than {@code v}
         *
         * @param v   the value to be compare with the function parameter
         * @param <T> the type of the value and parameter
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T extends Comparable<T>> F1<T, Boolean> lt(final T v) {
            return _cmp(v, true);
        }

        /**
         * Alias of {@link #lt(Comparable)}
         *
         * @since 0.2
         */
        public static <T extends Comparable<T>> F1<T, Boolean> lessThan(final T v) {
            return lt(v);
        }

        /**
         * Returns a function that apply to one parameter and compare it with the value {@code v} specified,
         * returns {@code true} if the parameter applied is greater than {@code v}
         *
         * @param v   the value to be compare with the function parameter
         * @param <T> the type of the value and parameter
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T extends Comparable<T>> F1<T, Boolean> gt(final T v) {
            return _cmp(v, false);
        }

        /**
         * Alias of {@link #gt(Comparable)}
         *
         * @since 0.2
         */
        public static <T extends Comparable<T>> F1<T, Boolean> greaterThan(final T v) {
            return gt(v);
        }

        /**
         * Returns a function that apply to one parameter and compare it with the value {@code v} specified,
         * returns {@code true} if the parameter applied is greater than or equals to {@code v}
         *
         * @param v   the value to be compare with the function parameter
         * @param <T> the type of the value and parameter
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T extends Comparable<T>> F1<T, Boolean> gte(final T v) {
            return negate(lt(v));
        }

        /**
         * Alias of {@link #gte(Comparable)}
         *
         * @since 0.2
         */
        public static <T extends Comparable<T>> F1<T, Boolean> greaterThanOrEqualsTo(final T v) {
            return gte(v);
        }

        /**
         * Returns a function that apply to one parameter and compare it with the value {@code v} specified,
         * returns {@code true} if the parameter applied is less than or equals to {@code v}
         *
         * @param v   the value to be compare with the function parameter
         * @param <T> the type of the value and parameter
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T extends Comparable<T>> F1<T, Boolean> lte(final T v) {
            return negate(gt(v));
        }

        /**
         * Alias of {@link #lte(Comparable)}
         *
         * @since 0.2
         */
        public static <T extends Comparable<T>> F1<T, Boolean> lessThanOrEqualsTo(final T v) {
            return lte(v);
        }

        /**
         * A function that apply to two parameters then return {@code true} if the first parameter
         * is less than the second one. The type of the two parameter should be the same and should
         * implements {@link Comparable}
         *
         * @since 0.2
         */
        public static final F2 LESS_THAN = _cmp(true);

        /**
         * A function that apply to two parameters then return {@code true} if the first parameter
         * is greater than the second one. The type of the two parameter should be the same and should
         * implements {@link Comparable}
         *
         * @since 0.2
         */
        public static final F2 GREATER_THAN = _cmp(false);

        /**
         * A function that apply to two parameters then return {@code true} if the first parameter
         * is less than or equal to the second one. The type of the two parameter should be the same
         * and should implements {@link Comparable}
         *
         * @since 0.2
         */
        public static final F2 LESS_THAN_OR_EQUAL_TO = negate(GREATER_THAN);

        /**
         * A function that apply to two parameters then return {@code true} if the first parameter
         * is greater than or equal to the second one. The type of the two parameter should be the same
         * and should implements {@link Comparable}
         *
         * @since 0.2
         */
        public static final F2 GREATER_THAN_OR_EQUAL_TO = negate(LESS_THAN);

        /**
         * Returns a function that check if a value is less than another one. This is the
         * type safe version of {@link #LESS_THAN}
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @return the function that do the comparison
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T extends Comparable<T>> F2<T, T, Boolean> lt() {
            return LESS_THAN;
        }

        /**
         * Alias of {@link #lt()}
         *
         * @since 0.2
         */
        public static <T extends Comparable<T>> F2<T, T, Boolean> lessThan() {
            return lt();
        }

        /**
         * Returns a function that check if a value is less than or equals to another one.
         * This is the type safe version of {@link #LESS_THAN_OR_EQUAL_TO}
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @return the function that do the comparison
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T extends Comparable<T>> F2<T, T, Boolean> lte() {
            return LESS_THAN_OR_EQUAL_TO;
        }

        /**
         * Alias of {@link #lte()}
         *
         * @since 0.2
         */
        public static <T extends Comparable<T>> F2<T, T, Boolean> lessThanOrEqualsTo() {
            return lte();
        }

        /**
         * Returns a function that check if a value is less than another one.
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @return the function that do the comparison
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T extends Comparable<T>> F2<T, T, Boolean> gt() {
            return GREATER_THAN;
        }

        /**
         * Alias of {@link #gt()}
         *
         * @since 0.2
         */
        public static <T extends Comparable<T>> F2<T, T, Boolean> greaterThan() {
            return gt();
        }

        /**
         * Returns a function that check if a value is greater than or equals to another one.
         * This is the type safe version of {@link #GREATER_THAN_OR_EQUAL_TO}
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T extends Comparable<T>> F2<T, T, Boolean> gte() {
            return negate(LESS_THAN);
        }

        /**
         * Alias of {@link #gte()}
         *
         * @since 0.2
         */
        public static <T extends Comparable<T>> F2<T, T, Boolean> greaterThanOrEqualsTo() {
            return gte();
        }


        private static <T> F2<T, T, Boolean> _cmp(final java.util.Comparator<? super T> c, final boolean lt) {
            return new F2<T, T, Boolean>() {
                @Override
                public Boolean apply(T t1, T t2) throws NotAppliedException, Break {
                    if (lt) {
                        return c.compare(t1, t2) < 0;
                    } else {
                        return c.compare(t1, t2) > 0;
                    }
                }
            };
        }

        /**
         * Returns a function that check if a value is less than another one using the {@link Comparator} specified
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @param c   The comparator that can compare the value
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> lt(final java.util.Comparator<? super T> c) {
            return _cmp(c, true);
        }

        /**
         * Alias of {@link #lt(java.util.Comparator)}
         *
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> lessThan(final java.util.Comparator<? super T> c) {
            return lt(c);
        }

        /**
         * Returns a function that check if a value is less than another one using the {@link Comparator} specified
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @param c   The comparator that can compare the value
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> gt(final java.util.Comparator<? super T> c) {
            return _cmp(c, false);
        }

        /**
         * Alias of {@link #gt(java.util.Comparator)}
         *
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> greaterThan(final java.util.Comparator<? super T> c) {
            return gt(c);
        }

        /**
         * Returns a function that check if a value is less than or equals to another one
         * using the {@link Comparator} specified
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @param c   The comparator that can compare the value
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> lte(final java.util.Comparator<? super T> c) {
            return negate(gt(c));
        }

        /**
         * Alias of {@link #lte(java.util.Comparator)}
         *
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> lessThanOrEqualsTo(final java.util.Comparator<? super T> c) {
            return lte(c);
        }

        /**
         * Returns a function that check if a value is greater than or equals to another one
         * using the {@link Comparator} specified
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @param c   The comparator that can compare the value
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> gte(final java.util.Comparator<? super T> c) {
            return negate(lt(c));
        }

        /**
         * Alias of {@link #gte(java.util.Comparator)}
         *
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> greaterThanOrEqualsTo(final java.util.Comparator<? super T> c) {
            return gte(c);
        }

        private static <T> F2<T, T, Boolean> _cmp(final Func2<? super T, ? super T, Integer> c, final boolean lt) {
            return new F2<T, T, Boolean>() {
                @Override
                public Boolean apply(T t1, T t2) throws NotAppliedException, Break {
                    if (lt) {
                        return c.apply(t1, t2) < 0;
                    } else {
                        return c.apply(t1, t2) > 0;
                    }
                }
            };
        }

        /**
         * Returns a function that check if a value is less than another one using the {@link Comparator} specified
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @param c   The comparator that can compare the value
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> lt(final Func2<? super T, ? super T, Integer> c) {
            return _cmp(c, true);
        }

        /**
         * Alias of {@link #lt(java.util.Comparator)}
         *
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> lessThan(final Func2<? super T, ? super T, Integer> c) {
            return lt(c);
        }

        /**
         * Returns a function that check if a value is less than another one using the {@link Comparator} specified
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @param c   The comparator that can compare the value
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> gt(final Func2<? super T, ? super T, Integer> c) {
            return _cmp(c, false);
        }

        /**
         * Alias of {@link #gt(java.util.Comparator)}
         *
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> greaterThan(final Func2<? super T, ? super T, Integer> c) {
            return gt(c);
        }

        /**
         * Returns a function that check if a value is less than or equals to another one
         * using the {@link Comparator} specified
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @param c   The comparator that can compare the value
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> lte(final Func2<? super T, ? super T, Integer> c) {
            return negate(gt(c));
        }

        /**
         * Alias of {@link #lte(java.util.Comparator)}
         *
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> lessThanOrEqualsTo(final Func2<? super T, ? super T, Integer> c) {
            return lte(c);
        }

        /**
         * Returns a function that check if a value is greater than or equals to another one
         * using the {@link Comparator} specified
         *
         * @param <T> The type of the value been compared, should implements {@link Comparable}
         * @param c   The comparator that can compare the value
         * @return the function that do the comparison
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> gte(final Func2<? super T, ? super T, Integer> c) {
            return negate(lt(c));
        }

        /**
         * Alias of {@link #gte(java.util.Comparator)}
         *
         * @since 0.2
         */
        public static <T> F2<T, T, Boolean> greaterThanOrEqualsTo(final Func2<? super T, ? super T, Integer> c) {
            return gte(c);
        }

        /**
         * A predefined function that applies to two parameters and check if they are equals to each other
         */
        public static final F2 EQ = new F2() {
            @Override
            public Object apply(Object a, Object b) {
                return _.eq(a, b);
            }
        };

        /**
         * Alias of {@link #EQ}
         *
         * @since 0.2
         */
        public static final F2 EQUAL = EQ;

        /**
         * The type-safe version of {@link #EQ}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <P1, P2> F2<P1, P2, Boolean> eq() {
            return EQ;
        }

        /**
         * Alias of {@link #eq()}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <P1, P2> F2<P1, P2, Boolean> equal() {
            return EQ;
        }

        /**
         * A predefined function that applies to two parameters and check if they are not equals to
         * each other. This is a negate function of {@link #EQ}
         *
         * @since 0.2
         */
        public static final F2 NE = negate(EQ);

        /**
         * Alias of {@link #NE}
         *
         * @since 0.2
         */
        public static final F2 NOT_EQUAL = NE;

        /**
         * The type-safe version of {@link #NE}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <P1, P2> F2<P1, P2, Boolean> ne() {
            return NE;
        }

        /**
         * Alias of {@link #ne()}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <P1, P2> F2<P1, P2, Boolean> notEqual() {
            return NE;
        }

        public static final Comparator NATURAL_ORDER = Comparator.NaturalOrderComparator.INSTANCE;

        public static final Comparator REVERSE_ORDER = reverseOrder();

        @SuppressWarnings("unchecked")
        public static <T extends Comparable<T>> Comparator<T> naturalOrder() {
            return (Comparator<T>) NATURAL_ORDER;
        }

        @SuppressWarnings("unchecked")
        public static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
            return _.comparator(Collections.reverseOrder());
        }

        public static <T> Comparator<T> reverse(final java.util.Comparator<? super T> c) {
            return new Comparator<T>() {
                @Override
                public int compare(T o1, T o2) {
                    return c.compare(o2, o1);
                }
            };
        }

        public static <T, U extends Comparable<? super U>> Comparator<T> comparing(
                final Function<? super T, ? extends U> keyExtractor
        ) {
            E.NPE(keyExtractor);
            return new Comparator<T>() {
                @Override
                public int compare(T o1, T o2) {
                    return (keyExtractor.apply(o1).compareTo(keyExtractor.apply(o2)));
                }
            };
        }

        public static <T, U> Comparator<T> comparing(
                final Function<? super T, ? extends U> keyExtractor,
                final java.util.Comparator<? super U> keyComparator
        ) {
            E.NPE(keyExtractor, keyComparator);
            return new Comparator<T>() {
                @Override
                public int compare(T o1, T o2) {
                    return keyComparator.compare(keyExtractor.apply(o1), keyExtractor.apply(o2));
                }
            };
        }

        /**
         * A predefined function that calculate hash code of an object
         *
         * @since 0.2
         */
        public static final F1 HASH_CODE = new F1() {
            @Override
            public Integer apply(Object o) {
                return _.hc(o);
            }
        };

        /**
         * The type-safe version of {@link #HASH_CODE}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T> F1<T, Integer> hc() {
            return HASH_CODE;
        }

        /**
         * A predefined function that when applied to an object instance, returns
         * String representation of the instance
         *
         * @see #toString(Object)
         * @since 0.2
         */
        public static final F1 AS_STRING = new F1() {
            @Override
            public Object apply(Object o) {
                return _.toString(o);
            }
        };

        /**
         * A type-safe version of {@link #AS_STRING}
         *
         * @since 0.2
         */
        @SuppressWarnings("unchecked")
        public static <T> F1<T, String> asString() {
            return AS_STRING;
        }

        public static <T> F1<T, String> asString(Class<T> tClass) {
            return AS_STRING;
        }
    }

    public static void main(String[] args) {
        None<String> none = none();
        System.out.println(none);

        F1<String, Integer> hc = _.F.hc();
        System.out.println(hc.apply("ABC"));

        F1<Integer, String> toString = _.F.asString();
        System.out.println(toString.apply(33));

        F0<String> f0 = _.F0;
        System.out.println(f0.apply());
    }

}