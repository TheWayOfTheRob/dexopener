package com.github.tmurakami.dexopener;

import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.iface.Annotation;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.iface.AnnotationElement;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.iface.ClassDef;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.iface.Field;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.iface.Method;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.iface.MethodParameter;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.iface.value.IntEncodedValue;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.immutable.ImmutableAnnotation;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.immutable.ImmutableAnnotationElement;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.immutable.ImmutableClassDef;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.immutable.ImmutableMethod;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.immutable.value.ImmutableIntEncodedValue;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.immutable.value.ImmutableStringEncodedValue;
import com.github.tmurakami.dexopener.repackaged.org.jf.dexlib2.rewriter.DexRewriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.then;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DexOpenerRewriterModuleTest {

    @Spy
    private DexOpenerRewriterModule testTarget;

    private DexRewriter rewriter;

    @Before
    public void setUp() throws Exception {
        rewriter = new DexRewriter(testTarget);
    }

    @Test
    public void classDefRewriter_should_remove_final_modifier_from_the_given_class() throws Exception {
        ClassDef in = new ImmutableClassDef("Lfoo/Bar;",
                                            Modifier.FINAL,
                                            "Ljava/lang/Object;",
                                            null,
                                            null,
                                            Collections.<Annotation>emptySet(),
                                            Collections.<Field>emptySet(),
                                            Collections.<Method>emptySet());
        ClassDef out = rewriter.getClassDefRewriter().rewrite(in);
        assertEquals(0, out.getAccessFlags());
        out.getType();
        then(testTarget).should().getTypeRewriter(rewriter);
    }

    @Test
    public void methodRewriter_should_remove_final_modifier_from_the_given_method() throws Exception {
        Method in = new ImmutableMethod("Lfoo/Bar;",
                                        "f",
                                        Collections.<MethodParameter>emptySet(),
                                        "V",
                                        Modifier.FINAL,
                                        Collections.<Annotation>emptySet(),
                                        null);
        Method out = rewriter.getMethodRewriter().rewrite(in);
        assertEquals(0, out.getAccessFlags());
        out.getName();
        then(testTarget).should().getMethodReferenceRewriter(rewriter);
    }

    @Test
    public void annotationRewriter_should_remove_final_modifier_from_the_given_inner_class_annotation() {
        Set<AnnotationElement> elements = new HashSet<>();
        elements.add(new ImmutableAnnotationElement("name", new ImmutableStringEncodedValue("Lfoo/Bar;")));
        elements.add(new ImmutableAnnotationElement("accessFlags", new ImmutableIntEncodedValue(Modifier.FINAL)));
        Annotation in = new ImmutableAnnotation(0, "Ldalvik/annotation/InnerClass;", elements);
        Annotation out = rewriter.getAnnotationRewriter().rewrite(in);
        int accessFlags = -1;
        for (AnnotationElement e : out.getElements()) {
            if (e.getName().equals("accessFlags")) {
                accessFlags = ((IntEncodedValue) e.getValue()).getValue();
            }
        }
        assertEquals(0, accessFlags);
    }

}