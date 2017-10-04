package org.exoplatform.services.injection;

import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.ExtendedCalendarService;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.common.jcr.KSDataLocation;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.wiki.service.WikiService;

abstract public class AbstractInjector {

    protected PortalContainer container;

    /** . */
    protected final IdentityManager identityManager;

    /** . */
    protected final IdentityStorage identityStorage;

    /** . */
    protected final RelationshipManager relationshipManager;

    /** . */
    protected final ActivityManager activityManager;

    /** . */
    protected final OrganizationService organizationService;

    /** . */
    protected final SpaceService spaceService;

    /** . */
    protected final WikiService wikiService;


    /** . */
    protected final ForumService forumService;

    /** . */
    protected final KSDataLocation ksDataLocation;

    protected final RepositoryService repositoryService;

    protected final SessionProviderService sessionProviderService;

    protected final ListenerService listenerService;

    protected final NodeHierarchyCreator nodeHierarchyCreator;

    protected final CalendarService calendarService;

    protected  final ExtendedCalendarService extendedCalendarService;


    public AbstractInjector() {

        this.container = PortalContainer.getInstance();
        this.identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);
        this.identityStorage = (IdentityStorage) container.getComponentInstanceOfType(IdentityStorage.class);
        this.relationshipManager = (RelationshipManager) container.getComponentInstanceOfType(RelationshipManager.class);
        this.activityManager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
        this.spaceService = (SpaceService) container.getComponentInstanceOfType(SpaceService.class);
        this.organizationService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);
        this.wikiService = (WikiService) container.getComponentInstanceOfType(WikiService.class);
        this.forumService = (ForumService) container.getComponentInstanceOfType(ForumService.class);
        this.ksDataLocation = (KSDataLocation) container.getComponentInstanceOfType(KSDataLocation.class);
        this.nodeHierarchyCreator = (NodeHierarchyCreator) container.getComponentInstanceOfType(NodeHierarchyCreator.class);
        this.listenerService = (ListenerService) container.getComponentInstanceOfType(ListenerService.class);
        this.sessionProviderService = (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);
        this.repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
        this.calendarService = (CalendarService) container.getComponentInstanceOfType(CalendarService.class);
        this.extendedCalendarService = (ExtendedCalendarService) container.getComponentInstanceOfType(ExtendedCalendarService.class);
    }
    public Log getLog() {
        return ExoLogger.getExoLogger(this.getClass());
    }
}
