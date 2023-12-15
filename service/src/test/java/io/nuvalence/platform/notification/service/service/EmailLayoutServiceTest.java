package io.nuvalence.platform.notification.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.nuvalence.platform.notification.service.domain.EmailLayout;
import io.nuvalence.platform.notification.service.exception.BadDataException;
import io.nuvalence.platform.notification.service.model.SearchEmailLayoutFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringBootTest
@ActiveProfiles("test")
class EmailLayoutServiceTest {

    @Autowired private EmailLayoutService service;

    @Test
    void testCreateEmailLayout() {
        final String key = "key";
        List<String> inputs =
                new ArrayList<>() {
                    {
                        add("input1");
                        add("input2");
                        add("input3");
                    }
                };
        EmailLayout emailLayout = new EmailLayout();
        emailLayout.setName("name");
        emailLayout.setDescription("description");
        emailLayout.setContent("content");
        emailLayout.setInputs(inputs);

        EmailLayout createdEmailLayout = service.createEmailLayout(key, emailLayout);

        assertNotNull(createdEmailLayout);
        assertEquals("DRAFT", createdEmailLayout.getStatus());
    }

    @Test
    void testCreateEmailLayout_content_contains_valid_inputs() {
        final String key = "key";

        final String content = "<div>{{input1}}</div>" + "<div>{{input2}}</div>";

        List<String> inputs =
                new ArrayList<>() {
                    {
                        add("input1");
                        add("input2");
                    }
                };
        EmailLayout emailLayout = new EmailLayout();
        emailLayout.setName("name");
        emailLayout.setDescription("description");
        emailLayout.setContent(content);
        emailLayout.setInputs(inputs);

        EmailLayout createdEmailLayout = service.createEmailLayout(key, emailLayout);

        assertNotNull(createdEmailLayout);
        assertEquals("DRAFT", createdEmailLayout.getStatus());
    }

    @Test
    void testCreateEmailLayout_content_contains_invalid_inputs() {
        final String key = "key";

        final String content = "<div>{{input1}}</div>" + "<div>{{invalidInput}}</div>";

        List<String> inputs =
                new ArrayList<>() {
                    {
                        add("input1");
                        add("input2");
                    }
                };
        EmailLayout emailLayout = new EmailLayout();
        emailLayout.setName("name");
        emailLayout.setDescription("description");
        emailLayout.setContent(content);
        emailLayout.setInputs(inputs);

        BadDataException thrown =
                Assertions.assertThrows(
                        BadDataException.class,
                        () -> {
                            service.createEmailLayout(key, emailLayout);
                        },
                        "BadDataException was expected");

        assertTrue(thrown.getMessage().contains("These inputs are not defined"));
    }

    @Test
    void testCreateEmailLayout_update() {
        final String key = "key";
        List<String> inputs =
                new ArrayList<>() {
                    {
                        add("input1");
                        add("input2");
                        add("input3");
                    }
                };
        EmailLayout emailLayout = new EmailLayout();
        emailLayout.setName("name");
        emailLayout.setDescription("description");
        emailLayout.setContent("content");
        emailLayout.setInputs(inputs);

        EmailLayout createdEmailLayout = service.createEmailLayout(key, emailLayout);

        assertNotNull(createdEmailLayout);
        assertEquals("DRAFT", createdEmailLayout.getStatus());

        EmailLayout emailLayout2 = new EmailLayout();
        emailLayout2.setName("name2");
        emailLayout2.setDescription("description2");
        emailLayout2.setContent("content");
        emailLayout2.setInputs(inputs);

        EmailLayout updatedEmailLayout = service.createEmailLayout(key, emailLayout2);

        assertNotNull(updatedEmailLayout);

        assertEquals(
                createdEmailLayout.getCreatedTimestamp(), updatedEmailLayout.getCreatedTimestamp());

        assertNotEquals(
                createdEmailLayout.getLastUpdatedTimestamp(),
                updatedEmailLayout.getLastUpdatedTimestamp());
    }

    @Test
    void testGetEmailLayoutByKey() {
        final String key = "key";
        List<String> inputs =
                new ArrayList<>() {
                    {
                        add("input1");
                        add("input2");
                        add("input3");
                    }
                };
        EmailLayout newEmailLayout = new EmailLayout();
        newEmailLayout.setName("name");
        newEmailLayout.setDescription("description");
        newEmailLayout.setContent("content");
        newEmailLayout.setInputs(inputs);

        EmailLayout createdEmailLayout = service.createEmailLayout(key, newEmailLayout);

        Optional<EmailLayout> emailLayout = service.getEmailLayout(key);

        assertThat(emailLayout).isNotEmpty();
        assertEquals(createdEmailLayout.getId(), emailLayout.get().getId());
    }

    @Test
    void testGetEmailLayouts() {
        final String key1 = "key";
        List<String> inputs1 =
                new ArrayList<>() {
                    {
                        add("input1-1");
                        add("input2-1");
                        add("input3-1");
                    }
                };
        EmailLayout newEmailLayout1 = new EmailLayout();
        newEmailLayout1.setName("name1");
        newEmailLayout1.setDescription("description1");
        newEmailLayout1.setContent("content1");
        newEmailLayout1.setInputs(inputs1);

        final String key2 = "key2";
        List<String> inputs2 =
                new ArrayList<>() {
                    {
                        add("input1-2");
                        add("input2-2");
                        add("input3-2");
                    }
                };
        EmailLayout newEmailLayout2 = new EmailLayout();
        newEmailLayout2.setName("name2");
        newEmailLayout2.setDescription("description2");
        newEmailLayout2.setContent("content2");
        newEmailLayout2.setInputs(inputs2);

        EmailLayout createdEmailLayout1 = service.createEmailLayout(key1, newEmailLayout1);
        EmailLayout createdEmailLayout2 = service.createEmailLayout(key2, newEmailLayout2);

        SearchEmailLayoutFilter filter =
                SearchEmailLayoutFilter.builder().name(newEmailLayout1.getName()).build();
        Page<EmailLayout> result = service.getEmailLayouts(filter);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(createdEmailLayout1.getId(), result.getContent().get(0).getId());
    }
}
