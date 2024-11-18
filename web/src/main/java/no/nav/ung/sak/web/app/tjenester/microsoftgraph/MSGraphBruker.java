package no.nav.ung.sak.web.app.tjenester.microsoftgraph;

import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.User;

import java.util.List;

public record MSGraphBruker(User bruker, List<Group> grupper) {
}
