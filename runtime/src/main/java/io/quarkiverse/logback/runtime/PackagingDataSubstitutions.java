package io.quarkiverse.logback.runtime;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import ch.qos.logback.classic.spi.PackagingDataCalculator;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

@TargetClass(PackagingDataCalculator.class)
public final class PackagingDataSubstitutions {

    @Substitute
    void populateFrames(StackTraceElementProxy[] stepArray) {

    }
}
