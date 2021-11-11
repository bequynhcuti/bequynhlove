/*
 * Copyright 2015 the original author or authors.
 *
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
 */
package org.gradle.api.tasks.diagnostics;

import com.google.common.annotations.VisibleForTesting;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.initialization.dsl.ScriptHandler;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.api.tasks.diagnostics.internal.DependencyReportRenderer;
import org.gradle.api.tasks.diagnostics.internal.ReportGenerator;
import org.gradle.api.tasks.diagnostics.internal.dependencies.AsciiDependencyReportRenderer;
import org.gradle.initialization.BuildClientMetaData;
import org.gradle.internal.logging.text.StyledTextOutputFactory;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;

import static java.util.Collections.singleton;

/**
 * Provides information about the build environment for the project that the task is associated with.
 * <p>
 * Currently, this information is limited to the project's declared build script dependencies
 * visualised in a similar manner as provided by {@link DependencyReportTask}.
 * <p>
 * It is not necessary to manually add a task of this type to your project,
 * as every project automatically has a task of this type by the name {@code "buildEnvironment"}.
 *
 * @since 2.10
 */
@DisableCachingByDefault(because = "Produces only non-cacheable console output")
@UntrackedTask(because = "Uses the project as an input")
public class BuildEnvironmentReportTask extends DefaultTask {

    public static final String TASK_NAME = "buildEnvironment";

    private DependencyReportRenderer renderer = new AsciiDependencyReportRenderer();

    @Inject
    protected BuildClientMetaData getClientMetaData() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected StyledTextOutputFactory getTextOutputFactory() {
        throw new UnsupportedOperationException();
    }

    @TaskAction
    public void generate() {
        reportGenerator().generateReport(
            singleton(getProject()),
            project -> {
                Configuration configuration = project.getBuildscript().getConfigurations().getByName(ScriptHandler.CLASSPATH_CONFIGURATION);
                renderer.startConfiguration(configuration);
                renderer.render(configuration);
                renderer.completeConfiguration(configuration);
            }
        );
    }

    private ReportGenerator reportGenerator() {
        return new ReportGenerator(renderer, getClientMetaData(), null, getTextOutputFactory());
    }

    @VisibleForTesting
    protected void setRenderer(DependencyReportRenderer dependencyReportRenderer) {
        this.renderer = dependencyReportRenderer;
    }
}
