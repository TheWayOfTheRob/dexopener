/*
 * Copyright 2016 Tsuyoshi Murakami
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tmurakami.dexopener;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.assertSame;
import static org.mockito.BDDMockito.given;

@RunWith(MockitoJUnitRunner.class)
public class DexOpenerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock(stubOnly = true)
    private Instrumentation instrumentation;
    @Mock(stubOnly = true)
    private Context context;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AndroidClassSourceFactory androidClassSourceFactory;

    @Test
    public void install_should_inject_the_class_source_into_the_given_class_loader()
            throws Exception {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        given(context.getApplicationInfo()).willReturn(applicationInfo);
        applicationInfo.sourceDir = "test";
        String dataDir = folder.newFolder().getCanonicalPath();
        applicationInfo.dataDir = dataDir;
        File cacheDir = new File(dataDir, "code_cache/dexopener");
        ClassLoader classLoader = new ClassLoader() {
        };
        given(context.getClassLoader()).willReturn(classLoader);
        given(androidClassSourceFactory.newClassSource("test", cacheDir)
                                       .getClassFile("foo.Bar")
                                       .toClass(classLoader)).willReturn(getClass());
        DexOpener.install(context, androidClassSourceFactory);
        assertSame(getClass(), classLoader.loadClass("foo.Bar"));
    }

    @Test(expected = IllegalStateException.class)
    public void install_should_throw_IllegalStateException_if_the_target_context_is_null() {
        given(instrumentation.getTargetContext()).willReturn(null);
        DexOpener.install(instrumentation);
    }

    @Test(expected = IllegalStateException.class)
    public void install_should_throw_IllegalStateException_if_the_Application_has_been_created() {
        given(instrumentation.getTargetContext()).willReturn(context);
        given(context.getApplicationContext()).willReturn(new Application());
        DexOpener.install(instrumentation);
    }

}