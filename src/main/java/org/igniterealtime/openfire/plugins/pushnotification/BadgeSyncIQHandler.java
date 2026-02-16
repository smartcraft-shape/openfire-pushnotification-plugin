package org.igniterealtime.openfire.plugins.pushnotification;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.handler.IQHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import java.util.HashSet;
import java.util.Iterator;

/**
 * IQ handler for badge sync. Allows the client to report its current set of
 * unread chat JIDs so the server can send accurate badge counts in push notifications.
 *
 * Expected stanza:
 * <pre>
 * &lt;iq type="set"&gt;
 *   &lt;badge-sync xmlns="urn:xmpp:push:badge-sync"&gt;
 *     &lt;chat jid="alice@domain.com"/&gt;
 *     &lt;chat jid="room@conference.domain.com"/&gt;
 *   &lt;/badge-sync&gt;
 * &lt;/iq&gt;
 * </pre>
 */
public class BadgeSyncIQHandler extends IQHandler
{
    private static final Logger Log = LoggerFactory.getLogger( BadgeSyncIQHandler.class );

    public static final String ELEMENT_NAME = "badge-sync";
    public static final String ELEMENT_NAMESPACE = "urn:xmpp:push:badge-sync";

    public BadgeSyncIQHandler()
    {
        super( "Push Badge Sync IQ Handler" );
    }

    @Override
    public IQ handleIQ( final IQ packet ) throws UnauthorizedException
    {
        if ( packet.isResponse() ) {
            return null;
        }

        if ( !IQ.Type.set.equals( packet.getType() ) )
        {
            final IQ result = IQ.createResultIQ( packet );
            result.setError( PacketError.Condition.bad_request );
            return result;
        }

        if ( !XMPPServer.getInstance().getUserManager().isRegisteredUser( packet.getFrom(), false ) )
        {
            throw new UnauthorizedException( "This service is only available to registered, local users." );
        }

        final String username = packet.getFrom().getNode();
        final Element badgeSync = packet.getChildElement();

        final HashSet<String> chatJids = new HashSet<>();
        for ( final Iterator<Element> it = badgeSync.elementIterator("chat"); it.hasNext(); )
        {
            final Element chat = it.next();
            final String jid = chat.attributeValue("jid");
            if ( jid != null && !jid.isEmpty() ) {
                chatJids.add(jid);
            }
        }

        Log.debug( "Badge sync for user '{}': {} unread chats", username, chatJids.size() );
        PushInterceptor.syncUnreadChats( username, chatJids );

        return IQ.createResultIQ( packet );
    }

    @Override
    public IQHandlerInfo getInfo()
    {
        return new IQHandlerInfo( ELEMENT_NAME, ELEMENT_NAMESPACE );
    }
}
