package com.cms.pattern.factory;

import com.cms.model.Certificate;
import com.cms.model.Conference;
import com.cms.model.User;
import org.springframework.stereotype.Component;

/** ConcreteCreator 2 — Presentation Certificate */
@Component
public class PresentationCertGenerator extends CertificateGenerator {

    @Override
    public Certificate.CertificateType getCertificateType() {
        return Certificate.CertificateType.PRESENTATION;
    }

    @Override
    protected String buildTitle() {
        return "Certificate of Presentation";
    }

    @Override
    protected String buildBody(User recipient, Conference conference) {
        return "This is to certify that " + recipient.getFullName()
                + " from " + institution(recipient)
                + " presented a research paper at " + conference.getName()
                + " and made a valuable contribution to the academic community.";
    }
}
