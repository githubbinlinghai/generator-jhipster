<%#
 Copyright 2013-2017 the original author or authors.

 This file is part of the JHipster project, see https://jhipster.github.io/
 for more information.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-%>
package <%=packageName%>.web.rest;
<% if (databaseType == 'cassandra') { %>
import <%=packageName%>.AbstractCassandraTest;<% } %>
import <%=packageName%>.<%= mainClass %>;<% if (databaseType == 'sql' || databaseType == 'mongodb') { %>
import <%=packageName%>.domain.Authority;<% } %>
import <%=packageName%>.domain.User;<% if (databaseType == 'sql' || databaseType == 'mongodb') { %>
import <%=packageName%>.repository.AuthorityRepository;<% } %>
<%_ if (authenticationType == 'session') { _%>
import <%=packageName%>.repository.PersistentTokenRepository;
<%_ } _%>
import <%=packageName%>.repository.UserRepository;
import <%=packageName%>.security.AuthoritiesConstants;
import <%=packageName%>.service.MailService;
import <%=packageName%>.service.UserService;
import <%=packageName%>.service.dto.UserDTO;
import <%=packageName%>.web.rest.vm.ManagedUserVM;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;<% if (databaseType == 'sql') { %>
import org.springframework.transaction.annotation.Transactional;<% } %>

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for the AccountResource REST controller.
 *
 * @see AccountResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = <%= mainClass %>.class)
public class AccountResourceIntTest <% if (databaseType == 'cassandra') { %>extends AbstractCassandraTest <% } %>{

    @Autowired
    private UserRepository userRepository;
<%_ if (databaseType == 'sql' || databaseType == 'mongodb') { _%>

    @Autowired
    private AuthorityRepository authorityRepository;
<%_ } _%>

    @Autowired
    private UserService userService;
<%_ if (authenticationType == 'session') { _%>

    @Autowired
    private PersistentTokenRepository persistentTokenRepository;
<%_ } _%>

    @Mock
    private UserService mockUserService;

    @Mock
    private MailService mockMailService;

    private MockMvc restUserMockMvc;

    private MockMvc restMvc;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(mockMailService).sendActivationEmail(anyObject());

        AccountResource accountResource =
            new AccountResource(userRepository, userService, mockMailService<% if (authenticationType == 'session') { %>, persistentTokenRepository<% } %>);

        AccountResource accountUserMockResource =
            new AccountResource(userRepository, mockUserService, mockMailService<% if (authenticationType == 'session') { %>, persistentTokenRepository<% } %>);

        this.restMvc = MockMvcBuilders.standaloneSetup(accountResource).build();
        this.restUserMockMvc = MockMvcBuilders.standaloneSetup(accountUserMockResource).build();
    }

    @Test
    public void testNonAuthenticatedUser() throws Exception {
        restUserMockMvc.perform(get("/api/authenticate")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string(""));
    }

    @Test
    public void testAuthenticatedUser() throws Exception {
        restUserMockMvc.perform(get("/api/authenticate")
            .with(request -> {
                request.setRemoteUser("test");
                return request;
            })
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().string("test"));
    }

    @Test
    public void testGetExistingAccount() throws Exception {<% if (databaseType == 'sql' || databaseType == 'mongodb') { %>
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.ADMIN);
        authorities.add(authority);<% } %><% if (databaseType == 'cassandra') { %>
        Set<String> authorities = new HashSet<>();
        authorities.add(AuthoritiesConstants.ADMIN);<% } %>

        User user = new User();
        user.setLogin("test");
        user.setFirstName("john");
        user.setLastName("doe");
        user.setEmail("john.doe@jhipster.com");
        <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
        user.setImageUrl("http://placehold.it/50x50");
        <%_ } _%>
        user.setLangKey("en");
        user.setAuthorities(authorities);
        when(mockUserService.getUserWithAuthorities()).thenReturn(user);

        restUserMockMvc.perform(get("/api/account")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.login").value("test"))
            .andExpect(jsonPath("$.firstName").value("john"))
            .andExpect(jsonPath("$.lastName").value("doe"))
            .andExpect(jsonPath("$.email").value("john.doe@jhipster.com"))
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            .andExpect(jsonPath("$.imageUrl").value("http://placehold.it/50x50"))
            <%_ } _%>
            .andExpect(jsonPath("$.langKey").value("en"))
            .andExpect(jsonPath("$.authorities").value(AuthoritiesConstants.ADMIN));
    }

    @Test
    public void testGetUnknownAccount() throws Exception {
        when(mockUserService.getUserWithAuthorities()).thenReturn(null);

        restUserMockMvc.perform(get("/api/account")
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRegisterValid() throws Exception {
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "joe",                  // login
            "password",             // password
            "Joe",                  // firstName
            "Shmoe",                // lastName
            "joe@example.com",      // email
            true,                   // activated
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            "http://placehold.it/50x50", //imageUrl
            <%_ } _%>
            "<%= nativeLanguage %>",                   // langKey
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            <%_ } _%>
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)));

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<User> user = userRepository.findOneByLogin("joe");
        assertThat(user.isPresent()).isTrue();
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRegisterInvalidLogin() throws Exception {
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,                   // id
            "funky-log!n",          // login <-- invalid
            "password",             // password
            "Funky",                // firstName
            "One",                  // lastName
            "funky@example.com",    // email
            true,                   // activated
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            "http://placehold.it/50x50", //imageUrl
            <%_ } _%>
            "<%= nativeLanguage %>",                   // langKey
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            <%_ } _%>
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)));

        restUserMockMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByEmail("funky@example.com");
        assertThat(user.isPresent()).isFalse();
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRegisterInvalidEmail() throws Exception {
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            "bob",              // login
            "password",         // password
            "Bob",              // firstName
            "Green",            // lastName
            "invalid",          // email <-- invalid
            true,               // activated
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            "http://placehold.it/50x50", //imageUrl
            <%_ } _%>
            "<%= nativeLanguage %>",                   // langKey
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            <%_ } _%>
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)));

        restUserMockMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user.isPresent()).isFalse();
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRegisterInvalidPassword() throws Exception {
        ManagedUserVM invalidUser = new ManagedUserVM(
            null,               // id
            "bob",              // login
            "123",              // password with only 3 digits
            "Bob",              // firstName
            "Green",            // lastName
            "bob@example.com",  // email
            true,               // activated
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            "http://placehold.it/50x50", //imageUrl
            <%_ } _%>
            "<%= nativeLanguage %>",                   // langKey
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            <%_ } _%>
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)));

        restUserMockMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByLogin("bob");
        assertThat(user.isPresent()).isFalse();
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRegisterDuplicateLogin() throws Exception {
        // Good
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "alice",                // login
            "password",             // password
            "Alice",                // firstName
            "Something",            // lastName
            "alice@example.com",    // email
            true,                   // activated
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            "http://placehold.it/50x50", //imageUrl
            <%_ } _%>
            "<%= nativeLanguage %>",                   // langKey
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            <%_ } _%>
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)));

        // Duplicate login, different email
        ManagedUserVM duplicatedUser = new ManagedUserVM(validUser.getId(), validUser.getLogin(), validUser.getPassword(), validUser.getFirstName(), validUser.getLastName(),
            "alicejr@example.com", true<% if (databaseType == 'mongodb' || databaseType == 'sql') { %>, validUser.getImageUrl()<% } %>, validUser.getLangKey()<% if (databaseType == 'mongodb' || databaseType == 'sql') { %>, validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate()<% } %>, validUser.getAuthorities());

        // Good user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        // Duplicate login
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andExpect(status().is4xxClientError());

        Optional<User> userDup = userRepository.findOneByEmail("alicejr@example.com");
        assertThat(userDup.isPresent()).isFalse();
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRegisterDuplicateEmail() throws Exception {
        // Good
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "john",                 // login
            "password",             // password
            "John",                 // firstName
            "Doe",                  // lastName
            "john@example.com",     // email
            true,                   // activated
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            "http://placehold.it/50x50", //imageUrl
            <%_ } _%>
            "<%= nativeLanguage %>",                   // langKey
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            <%_ } _%>
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER)));

        // Duplicate email, different login
        ManagedUserVM duplicatedUser = new ManagedUserVM(validUser.getId(), "johnjr", validUser.getPassword(), validUser.getLogin(), validUser.getLastName(),
            validUser.getEmail(), true<% if (databaseType == 'mongodb' || databaseType == 'sql') { %>, validUser.getImageUrl()<% } %>, validUser.getLangKey()<% if (databaseType == 'mongodb' || databaseType == 'sql') { %>, validUser.getCreatedBy(), validUser.getCreatedDate(), validUser.getLastModifiedBy(), validUser.getLastModifiedDate()<% } %>, validUser.getAuthorities());

        // Good user
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        // Duplicate email
        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(duplicatedUser)))
            .andExpect(status().is4xxClientError());

        Optional<User> userDup = userRepository.findOneByLogin("johnjr");
        assertThat(userDup.isPresent()).isFalse();
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRegisterAdminIsIgnored() throws Exception {
        ManagedUserVM validUser = new ManagedUserVM(
            null,                   // id
            "badguy",               // login
            "password",             // password
            "Bad",                  // firstName
            "Guy",                  // lastName
            "badguy@example.com",   // email
            true,                   // activated
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            "http://placehold.it/50x50", //imageUrl
            <%_ } _%>
            "<%= nativeLanguage %>",                   // langKey
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            <%_ } _%>
            new HashSet<>(Arrays.asList(AuthoritiesConstants.ADMIN)));

        restMvc.perform(
            post("/api/register")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(validUser)))
            .andExpect(status().isCreated());

        Optional<User> userDup = userRepository.findOneByLogin("badguy");
        assertThat(userDup.isPresent()).isTrue();
        assertThat(userDup.get().getAuthorities()).hasSize(1)
            .containsExactly(<% if (databaseType == 'sql' || databaseType == 'mongodb') { %>authorityRepository.findOne(AuthoritiesConstants.USER)<% } %><% if (databaseType == 'cassandra') { %>AuthoritiesConstants.USER<% } %>);
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testSaveInvalidLogin() throws Exception {
        UserDTO invalidUser = new UserDTO(
            null,                   // id
            "funky-log!n",          // login <-- invalid
            "Funky",                // firstName
            "One",                  // lastName
            "funky@example.com",    // email
            true,                   // activated
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            "http://placehold.it/50x50", //imageUrl
            <%_ } _%>
            "<%= nativeLanguage %>",                   // langKey
            <%_ if (databaseType == 'mongodb' || databaseType == 'sql') { _%>
            null,                   // createdBy
            null,                   // createdDate
            null,                   // lastModifiedBy
            null,                   // lastModifiedDate
            <%_ } _%>
            new HashSet<>(Arrays.asList(AuthoritiesConstants.USER))
        );

        restUserMockMvc.perform(
            post("/api/account")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(invalidUser)))
            .andExpect(status().isBadRequest());

        Optional<User> user = userRepository.findOneByEmail("funky@example.com");
        assertThat(user.isPresent()).isFalse();
    }
}
