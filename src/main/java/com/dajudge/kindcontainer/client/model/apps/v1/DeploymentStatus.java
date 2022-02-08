package com.dajudge.kindcontainer.client.model.apps.v1;

import com.dajudge.kindcontainer.client.model.base.BaseStatus;

public class DeploymentStatus extends BaseStatus<DeploymentCondition> {
    @Override
    public String toString() {
        return "DeploymentStatus{} " + super.toString();
    }
}
