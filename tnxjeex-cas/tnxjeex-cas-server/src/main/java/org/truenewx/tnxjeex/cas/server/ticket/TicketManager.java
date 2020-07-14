package org.truenewx.tnxjeex.cas.server.ticket;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.client.validation.Assertion;
import org.truenewx.tnxjee.service.Service;

/**
 * 票据管理器
 */
public interface TicketManager extends Service {

    String TGT_NAME = "CASTGC";
    String TICKET_GRANTING_TICKET_PREFIX = "TGT-";
    String SERVICE_TICKET_PREFIX = "ST-";

    void createTicketGrantingTicket(HttpServletRequest request, HttpServletResponse response);

    boolean validateTicketGrantingTicket(HttpServletRequest request, String service);

    String getServiceTicket(HttpServletRequest request, String service);

    Collection<ServiceTicket> findServiceTickets(HttpServletRequest request);

    Assertion validateServiceTicket(String service, String ticketId);
}
