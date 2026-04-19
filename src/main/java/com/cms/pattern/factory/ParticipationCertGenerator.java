package com.cms.pattern.factory;

import com.cms.model.Certificate;
import com.cms.model.Conference;
import com.cms.model.User;
import org.springframework.stereotype.Component;

/** ConcreteCreator 1 — Participation Certificate */
@Component
public class ParticipationCertGenerator extends CertificateGenerator {

    @Override
    public Certificate.CertificateType getCertificateType() {
        return Certificate.CertificateType.PARTICIPATION;
    }

    @Override
    protected String buildTitle() {
        return "Certificate of Participation";
    }

    @Override
    protected String buildBody(User recipient, Conference conference) {
        return "This is to certify that " + recipient.getFullName()
                + " from " + institution(recipient)
                + " has successfully participated in " + conference.getName() + ".";
    }
}
