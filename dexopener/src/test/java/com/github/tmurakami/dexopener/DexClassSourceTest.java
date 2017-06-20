package com.github.tmurakami.dexopener;

import com.github.tmurakami.dexopener.repackaged.com.github.tmurakami.classinjector.ClassFile;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import dalvik.system.DexFile;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DexClassSourceTest {

    @InjectMocks
    private DexClassSource testTarget;

    @Mock
    private DexFiles dexFiles;
    @Mock
    private DexClassFileFactory classFileFactory;
    @Mock
    private DexFile dexFile;
    @Mock
    private ClassFile classFile;

    @Test
    public void getClassFile_should_return_the_ClassFile_with_the_given_name() throws Exception {
        String className = "foo.Bar";
        given(dexFiles.get(className)).willReturn(dexFile);
        given(classFileFactory.newClassFile(className, dexFile)).willReturn(classFile);
        assertSame(classFile, testTarget.getClassFile(className));
    }

    @Test
    public void getClassFile_should_return_null_if_no_DexFile_with_the_given_name_can_be_found() throws Exception {
        assertNull(testTarget.getClassFile("foo.Bar"));
        then(classFileFactory).should(never()).newClassFile(anyString(), any(DexFile.class));
    }

}
