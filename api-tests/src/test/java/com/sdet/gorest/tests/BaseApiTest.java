package com.sdet.gorest.tests;

import com.sdet.gorest.client.UsersClient;
import com.sdet.gorest.extensions.GoRestExtension;
import com.sdet.gorest.service.UserSteps;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GoRestExtension.class)
public abstract class BaseApiTest {

    protected final UsersClient users = new UsersClient();
    protected final UserSteps userSteps = new UserSteps(users);
}
