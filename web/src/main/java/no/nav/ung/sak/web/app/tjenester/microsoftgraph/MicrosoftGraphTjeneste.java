package no.nav.ung.sak.web.app.tjenester.microsoftgraph;

import com.microsoft.graph.models.DirectoryObject;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.GroupCollectionResponse;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

@Dependent
public class MicrosoftGraphTjeneste {

    private final GraphServiceClient graphClient;


    @Inject
    public MicrosoftGraphTjeneste(MicrosoftGraphClientConfig microsoftGraphClientConfig) {
        this.graphClient = microsoftGraphClientConfig.getGraphClient();
    }

    public MSGraphBruker getUserInfoFromGraph(String userPrincipalName) {
        User user = graphClient.users().byUserId(userPrincipalName).get();

        return new MSGraphBruker(user, getUserGroupsFromGraph(userPrincipalName));
    }

    // TODO: Cache gruppene
    public List<Group> getUserGroupsFromGraph(String userPrincipalName) {
        GroupCollectionResponse groupCollectionResponse = graphClient
            .users()
            .byUserId(userPrincipalName)
            .memberOf()
            .graphGroup()
            .get();

        return Objects.requireNonNull(groupCollectionResponse.getValue())
            .stream()
            .filter(Objects::nonNull)
            .map(DirectoryObject.class::cast)
            .map(Group.class::cast)
            .toList();
    }
}
