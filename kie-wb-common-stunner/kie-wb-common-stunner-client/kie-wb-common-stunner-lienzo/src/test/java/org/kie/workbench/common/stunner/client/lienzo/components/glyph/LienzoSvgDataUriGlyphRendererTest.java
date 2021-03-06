/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.stunner.client.lienzo.components.glyph;

import com.ait.lienzo.client.core.shape.Group;
import com.ait.lienzo.test.LienzoMockitoTestRunner;
import com.google.gwt.safehtml.shared.SafeUri;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.stunner.core.client.shape.ImageDataUriGlyph;
import org.kie.workbench.common.stunner.core.client.shape.SvgDataUriGlyph;
import org.kie.workbench.common.stunner.core.client.util.SvgDataUriGenerator;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(LienzoMockitoTestRunner.class)
public class LienzoSvgDataUriGlyphRendererTest {

    private static final String URI_B64 = "data:image/svg+xml;base64,c3ZnLWNvbnRlbnQ=";
    private static final SvgDataUriGenerator DATA_URI_UTIL = new SvgDataUriGenerator();

    @Mock
    private SafeUri uri;

    @Mock
    private LienzoPictureGlyphRenderer lienzoPictureGlyphRenderer;

    @Mock
    private Group group;

    private LienzoSvgDataUriGlyphRenderer tested;
    private SvgDataUriGlyph glyph;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() throws Exception {
        when(uri.asString()).thenReturn(URI_B64);
        when(lienzoPictureGlyphRenderer.render(any(ImageDataUriGlyph.class),
                                               anyDouble(),
                                               anyDouble())).thenReturn(group);
        when(lienzoPictureGlyphRenderer.render(anyString(),
                                               anyDouble(),
                                               anyDouble())).thenReturn(group);
        this.glyph = SvgDataUriGlyph.Builder.build(uri);
        this.tested = new LienzoSvgDataUriGlyphRenderer(DATA_URI_UTIL,
                                                        lienzoPictureGlyphRenderer);
    }

    @Test
    public void testType() {
        assertEquals(SvgDataUriGlyph.class,
                     tested.getGlyphType());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRender() {
        final Group glyphView = tested.render(glyph,
                                              100,
                                              200);
        assertEquals(group,
                     glyphView);
        final ArgumentCaptor<String> imageGlyphCaptor = ArgumentCaptor.forClass(String.class);
        verify(lienzoPictureGlyphRenderer,
               times(1)).render(imageGlyphCaptor.capture(),
                                eq(100d),
                                eq(200d));
        assertEquals(URI_B64,
                     imageGlyphCaptor.getValue());
    }
}
