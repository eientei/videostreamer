<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="forms" uri="http://www.springframework.org/tags/form" %>
<div class="fancybox">
    <h1>Welcome back, <sec:authentication property="principal.username"/></h1>
    <form:form action="/profile" method="post" commandName="profileForm">
        <div class="fancybox darker">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <table>
                <tr>
                    <td>
                        <form:password path="passwordOriginal"/>
                    </td>
                    <td>
                        Original password
                    </td>
                    <td class="errors">
                        <form:errors path="passwordOriginal"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:password path="password"/>
                    </td>
                    <td>
                        New Password
                    </td>
                    <td class="errors">
                        <form:errors path="password"/>
                    </td
                </tr>
                <tr>
                    <td>
                        <form:password path="passwordRepeat"/>
                    </td>
                    <td>
                        Repeat Password
                    </td>
                    <td class="errors">
                        <form:errors path="passwordRepeat"/>
                    </td
                </tr>
                <tr>
                    <td colspan="2">
                        <button type="submit" name="action" value="password">Change password</button>
                    </td>
                </tr>
            </table>
        </div>
        <div class="fancybox darker">
            <table>
                <tr>
                    <td>
                        <form:input path="email"/>
                    </td>
                    <td>
                        Email
                    </td>
                    <td class="errors">
                        <form:errors path="email"/>
                    </td
                </tr>
                <tr>
                    <td colspan="2">
                        <button type="submit" name="action" value="email">Change email</button>
                    </td>
                </tr>
            </table>
        </div>
        <div class="fancybox darker">
            <c:set var="req" value="${pageContext.request}" />
            <c:set var="baseURL" value="${fn:replace(req.requestURL, fn:substring(req.requestURI, 0, fn:length(req.requestURI)), req.contextPath)}" />
            <jsp:useBean id="profileForm" scope="request" type="org.eientei.video.form.ProfileForm"/>
            <div>
                <span>${profileForm.stream.token}</span>
                <button type="submit" name="action" value="streamToken">Generate new token</button>
            </div>
            <div>
                <input type="hidden" name="stream.id" value="${profileForm.stream.id}"/>
                <input type="hidden" name="stream.token" value="${profileForm.stream.token}"/>
                live / <input type="text" name="stream.name" value="${profileForm.stream.name}"/>
                <button type="submit" name="action" value="streamUpdate">Update</button>
                <span class="errors"><forms:errors path="stream.name"/></span>
            </div>
            <div>Publish url: <a href="${rtmpBase}/live/${profileForm.stream.token}">${rtmpBase}/live/${profileForm.stream.token}</a></div>
            <div>View url: <a href="${rtmpBase}/live/${profileForm.stream.name}">${rtmpBase}/live/${profileForm.stream.name}</a></div>
            <div>Web url: <a href="${baseURL}/live/${profileForm.stream.name}">${baseURL}/live/${profileForm.stream.name}</a></div>
            <c:choose>
                <c:when test="${not empty profileForm.stream.remote}">
                    <div>Status: Online ( ${profileForm.stream.remote} )</div>
                </c:when>
                <c:otherwise>
                    <div>Status: Offline</div>
                </c:otherwise>
            </c:choose>
        </div>
    </form:form>
</div>