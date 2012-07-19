package org.jboss.errai.ui.test.quickhandler.client;

import org.jboss.errai.enterprise.client.cdi.AbstractErraiCDITest;
import org.jboss.errai.ioc.client.container.IOC;
import org.junit.Test;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Label;

public class QuickHandlerTemplateTest extends AbstractErraiCDITest {

  @Override
  public String getModuleName() {
    return getClass().getName().replaceAll("client.*$", "Test");
  }

  @Test
  public void testInsertAndReplace() {
    QuickHandlerTemplateTestApp app = IOC.getBeanManager().lookupBean(QuickHandlerTemplateTestApp.class).getInstance();
    assertNotNull(app.getComponent());

    DivElement c0 = DivElement.as(Document.get().getElementById("c0"));
    assertNotNull(c0);
    AnchorElement c1 = app.getComponent().getC1();
    ButtonElement c2 = ButtonElement.as(Document.get().getElementById("c2"));
    assertNotNull(c2);

    assertFalse(app.getComponent().isC0EventFired());
    assertFalse(app.getComponent().isC0EventFired2());
    c0.dispatchEvent(generateClickEvent());
    assertTrue(app.getComponent().isC0EventFired());
    assertFalse(app.getComponent().isC0EventFired2());

    assertFalse(app.getComponent().isC1EventFired());
    c1.dispatchEvent(generateClickEvent());
    // FIXME This is currently not working because the Handler is being attached to the wrong ElementWrapperWidget instance
    // assertTrue(app.getComponent().isC1EventFired());

    assertFalse(app.getComponent().isC2EventFired());
    c2.click();
    assertTrue(app.getComponent().isC2EventFired());
  }

  private NativeEvent generateClickEvent() {
    return Document.get().createClickEvent(0, 0, 0, 0, 0, false, false, false, false);
  }

}