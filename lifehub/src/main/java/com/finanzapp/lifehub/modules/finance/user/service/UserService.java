package com.finanzapp.user.service;

import com.finanzapp.user.service.domain.*;

public interface UserService {

    /**
     * Checks if a user with the given email has completed the onboarding process.
     *
     * @param email the email of the user to check
     * @return a BooleanResponse indicating whether the user has onboarded
     */
    BooleanResponse hasOnBoarded(String email);

    /**
     * Creates a new user based on the provided request data.
     *
     * @param request the request containing user details
     */
    void createUser(CreateUserRequest request);

    /**
     * Retrieves the ideal percentage allocation for the user with the given email.
     * @param email the email of the user
     * @return the ideal percentage response
     */
    IdealPercentageResponse getIdealPercentage(String email);

    /**
     * Updates the user information based on the provided request data.
     * @param user the request containing updated user details
     * @param email the email of the user to update
     * @return the updated user
     */
    User updateUser(UpdateUserRequest user, String email);

    User getUserByEmail(String email);
}
