package com.dajudge.kindcontainer.webhook;

public interface AdmissionControllerBuilder {
    ValidatingAdmissionControllerBuilder validating();

    MutatingAdmissionControllerBuilder mutating();
}
