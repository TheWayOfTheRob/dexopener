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

import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.Opcodes;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.iface.ClassDef;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.iface.DexFile;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.immutable.ImmutableClassDef;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.immutable.ImmutableDexFile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Answer2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AndroidClassSourceTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Mock(stubOnly = true)
    private ClassNameFilter classNameFilter;
    @Mock(stubOnly = true)
    private ClassOpener classOpener;

    @Captor
    private ArgumentCaptor<Set<? extends ClassDef>> classesCaptor;

    @Test
    public void getClassFile_should_return_the_ClassFile_with_the_given_name() throws Exception {
        Opcodes opcodes = Opcodes.getDefault();
        given(classNameFilter.accept(matches("foo[.]Bar[\\d]{1,3}"))).willReturn(true);
        given(classOpener.openClasses(eq(opcodes), classesCaptor.capture()))
                .willAnswer(answer(new Answer2<RunnableFuture<? extends dalvik.system.DexFile>, Opcodes, Set<? extends ClassDef>>() {
                    @Override
                    public RunnableFuture<? extends dalvik.system.DexFile> answer(Opcodes ops,
                                                                                  final Set<? extends ClassDef> classes) {
                        return new FutureTask<>(new MockDexFileFactory(classes));
                    }
                }));
        int classCount = 101; // DexFileHolderMapper#MAX_CLASSES_PER_DEX_FILE + 1
        List<String> classNames = new ArrayList<>(classCount);
        for (int i = 0; i < classCount; i++) {
            classNames.add("foo.Bar" + i);
        }
        Set<ClassDef> classes = new HashSet<>();
        for (String className : classNames) {
            classes.add(new ImmutableClassDef('L' + className.replace('.', '/') + ';',
                                              0,
                                              null,
                                              null,
                                              null,
                                              null,
                                              null,
                                              null));
        }
        DexFile dexFile = new ImmutableDexFile(Opcodes.getDefault(), classes);
        byte[] bytecode = DexPoolUtils.toBytecode(dexFile);
        File apk = generateZip(bytecode);
        AndroidClassSource classSource = new AndroidClassSource(opcodes,
                                                                apk.getCanonicalPath(),
                                                                classNameFilter,
                                                                classOpener);
        for (String className : classNames) {
            assertNotNull(classSource.getClassFile(className));
        }
        List<Set<? extends ClassDef>> classesValues = classesCaptor.getAllValues();
        assertSame(2, classesValues.size());
        assertSame(100, classesValues.get(0).size());
        assertSame(1, classesValues.get(1).size());
    }

    @Test
    public void getClassFile_should_return_null_if_the_given_name_does_not_pass_through_the_filter()
            throws Exception {
        AndroidClassSource classSource = new AndroidClassSource(Opcodes.getDefault(),
                                                                "",
                                                                classNameFilter,
                                                                classOpener);
        assertNull(classSource.getClassFile("foo.Bar"));
    }

    @Test(expected = IllegalStateException.class)
    public void getClassFile_should_throw_IllegalStateException_if_no_class_to_be_opened_was_found()
            throws Exception {
        String className = "foo.Bar";
        given(classNameFilter.accept(className)).willReturn(true);
        DexFile dexFile = new ImmutableDexFile(Opcodes.getDefault(), Collections.<ClassDef>emptySet());
        byte[] bytecode = DexPoolUtils.toBytecode(dexFile);
        File apk = generateZip(bytecode);
        new AndroidClassSource(Opcodes.getDefault(),
                               apk.getCanonicalPath(),
                               classNameFilter,
                               classOpener).getClassFile(className);
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private File generateZip(byte[] bytecode) throws IOException {
        File zip = folder.newFile();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip));
        try {
            out.putNextEntry(new ZipEntry("classes.dex"));
            out.write(bytecode);
        } finally {
            out.close();
        }
        return zip;
    }

    private static class MockDexFileFactory implements Callable<dalvik.system.DexFile> {

        private final Set<? extends ClassDef> classes;

        MockDexFileFactory(Set<? extends ClassDef> classes) {
            this.classes = classes;
        }

        @Override
        public dalvik.system.DexFile call() {
            final List<String> classNames = new ArrayList<>(classes.size());
            for (ClassDef def : classes) {
                String dexName = def.getType();
                classNames.add(dexName.substring(1, dexName.length() - 1).replace('/', '.'));
            }
            dalvik.system.DexFile dexFile = mock(dalvik.system.DexFile.class, withSettings().stubOnly());
            given(dexFile.entries()).willAnswer(new Answer<Enumeration<String>>() {
                @Override
                public Enumeration<String> answer(InvocationOnMock invocation) {
                    return Collections.enumeration(classNames);
                }
            });
            return dexFile;
        }

    }

}
