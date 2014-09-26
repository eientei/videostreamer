<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<div class="fancybox">
    <div class="signup">
        <form:form action="/signup" method="post" commandName="signupForm">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <div class="errors">
                <form:errors/>
            </div>
            <table>
                <tr>
                    <td>
                        <form:input path="username"/>
                    </td>
                    <td>
                        <form:label path="username">Username (3..64 characters)</form:label>
                    </td>
                    <td class="errors">
                        <form:errors path="username"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:password path="password"/>
                    </td>
                    <td>
                        <form:label path="password">Password (3..64 characters)</form:label>
                    </td>
                    <td class="errors">
                        <form:errors path="password"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:password path="passwordRepeat"/>
                    </td>
                    <td>
                        <form:label path="passwordRepeat">Repeat password</form:label>
                    </td>
                    <td class="errors">
                        <form:errors path="passwordRepeat"/>
                    </td>
                </tr>
                <tr>
                    <td>
                        <form:input path="email"/>
                    </td>
                    <td>
                        <form:label path="email">Email (optional)</form:label>
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
                        <input type="submit" value="Sign Up"/>
                    </td>
                </tr>
            </table>
        </form:form>
    </div>
</div>