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

import com.github.tmurakami.dexopener.repackaged.com.github.tmurakami.classinjector.ClassFile;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.Opcodes;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.iface.ClassDef;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.iface.DexFile;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.immutable.ImmutableClassDef;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.immutable.ImmutableDexFile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.VoidAnswer2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.AdditionalAnswers.answerVoid;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.will;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AndroidClassSourceTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Mock(stubOnly = true)
    private ClassNameFilter classNameFilter;
    @Mock(stubOnly = true)
    private DexFileHolderMapper dexFileHolderMapper;
    @Mock(stubOnly = true)
    private DexFileHolder dexFileHolder;
    @Mock(stubOnly = true, answer = Answers.RETURNS_DEEP_STUBS)
    private DexClassSourceFactory dexClassSourceFactory;
    @Mock(stubOnly = true)
    private ClassFile classFile;

    @Captor
    private ArgumentCaptor<byte[]> bytecodeCaptor;

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    @Test
    public void getClassFile_should_return_the_ClassFile_with_the_given_name() throws Exception {
        final String className = "foo.Bar";
        given(classNameFilter.accept(className)).willReturn(true);
        will(answerVoid(new VoidAnswer2<byte[], Map<String, DexFileHolder>>() {
            @Override
            public void answer(byte[] bytecode, Map<String, DexFileHolder> holderMap) {
                holderMap.put(className, dexFileHolder);
            }
        })).given(dexFileHolderMapper).map(bytecodeCaptor.capture(), ArgumentMatchers.<String, DexFileHolder>anyMap());
        given(dexClassSourceFactory
                      .newClassSource(argThat(new ArgumentMatcher<Map<String, DexFileHolder>>() {
                          @Override
                          public boolean matches(Map<String, DexFileHolder> holderMap) {
                              return holderMap.size() == 1 &&
                                     holderMap.containsKey(className) &&
                                     holderMap.containsValue(dexFileHolder);
                          }
                      }))
                      .getClassFile(className)).willReturn(classFile);
        ClassDef def = new ImmutableClassDef("Lfoo/Bar;",
                                             0,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null,
                                             null);
        DexFile dexFile = new ImmutableDexFile(Opcodes.getDefault(), Collections.singleton(def));
        byte[] bytecode = DexPoolUtils.toBytecode(dexFile);
        File apk = generateZip(bytecode);
        assertSame(classFile, new AndroidClassSource(apk.getCanonicalPath(),
                                                     classNameFilter,
                                                     dexFileHolderMapper,
                                                     dexClassSourceFactory).getClassFile(className));
        assertArrayEquals(bytecode, bytecodeCaptor.getValue());
    }

    @Test
    public void getClassFile_should_return_null_if_the_given_name_does_not_pass_through_the_filter()
            throws Exception {
        AndroidClassSource classSource = new AndroidClassSource("",
                                                                classNameFilter,
                                                                dexFileHolderMapper,
                                                                dexClassSourceFactory);
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
        new AndroidClassSource(apk.getCanonicalPath(),
                               classNameFilter,
                               dexFileHolderMapper,
                               dexClassSourceFactory).getClassFile(className);
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

}
