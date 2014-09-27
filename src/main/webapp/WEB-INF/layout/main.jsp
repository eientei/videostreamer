<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<html>
<head>
    <title>
        <tiles:getAsString name="title"/>
    </title>
    <meta name="_csrf" content="${_csrf.token}"/>
    <meta name="_csrf_header" content="${_csrf.headerName}"/>
    <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-1.11.0.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery-ui.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/js/jquery.cookie.js"></script>
    <script type="text/javascript" src="${pageContext.request.contextPath}/js/favico.js"></script>
    <script type="text/javascript" src="http://www.google.com/recaptcha/api/js/recaptcha_ajax.js"></script>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/favicon.ico" sizes="16x16"/>
    <link href="${pageContext.request.contextPath}/css/style.css" rel="stylesheet"/>
</head>
<body>
<div class="toppanel">
    <div class="left">
        <jsp:useBean id="streamtitle" scope="request" type="java.lang.String"/>
        <jsp:useBean id="streameditable" scope="request" type="java.lang.Boolean"/>
        <span class="streamtitle${streameditable ? ' editable' : ''}">
            <c:if test="${showtitle}">
                <c:choose>
                    <c:when test="${empty streamtitle}">
                        <em>No title</em>
                    </c:when>
                    <c:otherwise>
                        ${streamtitle}
                    </c:otherwise>
                </c:choose>
            </c:if>
        </span>
    </div>
    <div class="right">
        <jsp:useBean id="userhash" scope="request" type="java.lang.String"/>
        <a href="${pageContext.request.contextPath}/">Home</a>
        <a href="${pageContext.request.contextPath}/info">Info</a>
        <sec:authorize access="isAnonymous()">
            <a href="${pageContext.request.contextPath}/signup">Signup</a>
            <a href="${pageContext.request.contextPath}/login">Login</a>
        </sec:authorize>
        <sec:authorize access="isAuthenticated()">
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout">Logout</a>
        </sec:authorize>
        <img class="hashremote" width="16" height="16" src="http://www.gravatar.com/avatar/${userhash}?d=identicon&s=64" />
    </div>
</div>
<tiles:insertAttribute name="body"/>
</body>
</html>
