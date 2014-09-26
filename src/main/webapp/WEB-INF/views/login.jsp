<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<div class="fancybox">
    <form:form action="/login" method="post" commandName="loginForm">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <div class="errors">
                <form:errors/>
            </div>
        <table>
            <tr>
                <td>
                    <form:input path="username" />
                </td>
                <td>
                    Username
                </td>
            </tr>
            <tr>
                <td>
                    <form:password path="password" />
                </td>
                <td>
                    Password
                </td>
            </tr>
            <c:if test="${captcha}">
                <tr>
                    <td colspan="2">
                        <div id="recpatcha_div"></div>
                        <script type="application/javascript">
                            Recaptcha.create("${reCaptchaPublic}", "recpatcha_div", { theme: "red" } );
                        </script>
                    </td>
                </tr>
            </c:if>
            <tr>
                <td colspan="2">
                    <button type="submit">Log In</button>
                </td>
            </tr>
        </table>
    </form:form>
</div>