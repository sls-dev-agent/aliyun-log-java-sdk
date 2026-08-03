package com.aliyun.openservices.log.request;

import com.aliyun.openservices.log.common.ResourcePolicyResourceType;

/**
 * The request used to delete a resource policy.
 */
public class DeleteResourcePolicyRequest extends Request {
    private static final long serialVersionUID = 1L;

    private ResourcePolicyResourceType resourceType;
    private String resourceName;

    public DeleteResourcePolicyRequest(String project, ResourcePolicyResourceType resourceType,
                                       String resourceName) {
        super(project);
        this.resourceType = resourceType;
        this.resourceName = resourceName;
    }

    public ResourcePolicyResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourcePolicyResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}
