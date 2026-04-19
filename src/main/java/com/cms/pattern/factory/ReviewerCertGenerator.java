package com.cms.pattern.factory;

import com.cms.model.Certificate;
import com.cms.model.Conference;
import com.cms.model.User;
import org.springframework.stereotype.Component;

/** ConcreteCreator 4 — Reviewer Certificate */
@Component
public class ReviewerCertGenerator extends CertificateGenerator {

    @Override
    public Certificate.CertificateType getCertificateType() {
        return Certificate.CertificateType.REVIEWER;
    }

    @Override
    protected String buildTitle() {
        return "Certificate of Review";
    }

    @Override
    protected String buildBody(User recipient, Conference conference) {
        return "This is to acknowledge that " + recipient.getFullName()
                + " from " + institution(recipient)
                + " served as a Peer Reviewer for " + conference.getName()
                + " and contributed to maintaining the quality of academic research.";
    }
}
