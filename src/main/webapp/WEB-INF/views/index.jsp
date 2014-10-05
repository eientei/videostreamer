<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="rtmpBase" scope="request" type="java.lang.String"/>
<jsp:useBean id="activeStreams" scope="request" type="java.util.List"/>
<div class="fancybox">
    <div class="streams">
        <c:choose>
            <c:when test="${empty activeStreams}">
                <h1>Sorry, no streams running</h1>
            </c:when>
            <c:otherwise>
                <h1>Running streams:</h1>
                <ul>
                    <c:forEach items="${activeStreams}" var="stream">
                        <li>
                            [ <a href="/${stream.app}/${stream.name}">${stream.app}/${stream.name}</a>
                            <c:if test="${not empty stream.topic}">
                                - "${stream.topic}"
                            </c:if>
                            ]
                            [
                            <a href="/${stream.app}/${stream.name}?buffer=0.0">unbuffered</a>
                            ]
                            [ <a href="${rtmpBase}/${stream.app}/${stream.name}">direct rtmp url</a> ]
                            [ <a href="/${stream.app}/${stream.name}?novideo=1">no video</a> ]
                            [ <a href="/${stream.app}/${stream.name}?nochat=1">no chat</a> ]
                            author: ${stream.author.name}
                            ( <img src="http://www.gravatar.com/avatar/${stream.hash}?d=identicon&s=64" width="32" height="32" alt="${stream.hash}" /> )
                            watching: ${stream.users}
                        </li>
                    </c:forEach>
                </ul>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<div class="center">Github: <a href="https://github.com/eientei/videostreamer">https://github.com/eientei/videostreamer</a></div>

