public type ProjectStatusBase record {
    *ProjectStatusCompact;
    UserCompact author?;
    # The time at which this project status was last modified.
    anydata modified_at?;
    # The text content of the status update.
    string text?;
    string html_text?;
    # The color associated with the status update.
    string color?;
};

public type ProjectStatusRequest record {
    *ProjectStatusBase;
};

# A user object represents an account in Asana that can be given access to various workspaces, projects, and tasks.
public type UserCompact record {
    *AsanaResource;
    # Read-only except when same user as requester.
    string name?;
};

# A generic Asana Resource, containing a globally unique identifier.
public type AsanaResource record {
    # Globally unique identifier of the resource, as a string.
    string gid?;
    # The base type of this resource.
    string resource_type?;
};

public type Body record {
    ProjectStatusRequest data?;
};

# A project status is an update on the progress of a particular project, and is sent out to all project followers when created.
public type ProjectStatusCompact record {
    *AsanaResource;
    # The title of the project status update.
    string title?;
};
