package org.osgl.util;

/*-
 * #%L
 * Java Tool
 * %%
 * Copyright (C) 2014 - 2017 OSGL (Open Source General Library)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.Clock;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@BenchmarkOptions(benchmarkRounds = 15000, warmupRounds = 1000, clock = Clock.NANO_TIME)
@BenchmarkMethodChart(filePrefix = "map-types-benchmark-barchart")
public class StringConcatBenchmark extends AbstractBenchmark {

    private static String s1;
    private static String s2;
    private static String s3;
    private static String s4;
    private static String s5;
    private static String s6;
    private static String s12;
    private static int i12;
    private static String s123;
    private static int i123;
    private static String s123456;
    private static int i123456;

    @BeforeClass
    public static void prepare() {
        s1 = S.random();
        s2 = S.random();
        s3 = S.random();
        s4 = S.random();
        s5 = S.random();
        s6 = S.random();
        s12 = s1 + s2;
        i12 = s12.length();
        s123 = s12 + s3;
        i123 = s123.length();
        s123456 = s123 + s4 + s5 + s6;
        i123456 = s123456.length();
    }

    @Test
    public void _ignore() {
        String s = new StringBuilder(s1).append(s2).append(s3).append(s4).append(s5).append(s6).toString();
        for (int i = 0; i < 1000; ++i) {
            s = new StringBuilder(s1).append(s2).append(s3).append(s4).append(s5).append(s6).toString();
        }
        Assert.assertSame(i123456, s.length());
    }

    @Test
    public void concat6a() {
        String s = S.concat(s1, s2, s3, s4, s5, s6);
        for (int i = 0; i < 1000; ++i) {
            s = S.concat(s1, s2, s3, s4, s5, s6);
        }
        Assert.assertSame(i123456, s.length());
    }

}
