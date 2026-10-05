package com.sdet.gorest.data;

import com.sdet.gorest.config.RunContext;
import com.sdet.gorest.model.Gender;
import com.sdet.gorest.model.Status;
import com.sdet.gorest.model.User;
import net.datafaker.Faker;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dynamic test data. Every name/email carries the run id + a sequence + random part, so
 * reruns (and concurrent CI runs) never collide and our records are always recognisable.
 */
public final class UserFactory {

    private static final Faker FAKER = new Faker();
    private static final AtomicInteger SEQ = new AtomicInteger();
    public static final String EMAIL_DOMAIN = "example.com";

    private UserFactory() {
    }

    public static User randomUser() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        return new User()
                .withName(uniqueName())
                .withEmail(uniqueEmail())
                .withGender(Gender.values()[rnd.nextInt(Gender.values().length)])
                .withStatus(Status.values()[rnd.nextInt(Status.values().length)]);
    }

    /** A completely different valid user: new name and email, flipped gender and status. */
    public static User differentFrom(User original) {
        return randomUser()
                .withGender(Gender.of(original.getGender()).opposite())
                .withStatus(Status.of(original.getStatus()).opposite());
    }

    public static String uniqueTag() {
        return "sdet-" + RunContext.runId() + "-" + SEQ.incrementAndGet();
    }

    public static String uniqueName() {
        return FAKER.name().firstName() + " " + FAKER.name().lastName() + " " + uniqueTag();
    }

    public static String uniqueEmail() {
        return "sdet." + RunContext.runId() + "." + SEQ.incrementAndGet() + "."
                + UUID.randomUUID().toString().substring(0, 8) + "@" + EMAIL_DOMAIN;
    }
}
