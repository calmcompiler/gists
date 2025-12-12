/**
 * Validates whether a given string is a valid Terraform resource name.
 *
 * <p>Terraform resource names must follow strict naming conventions:
 * <ul>
 *   <li>Allowed characters: lowercase letters (<code>a-z</code>), digits (<code>0-9</code>), underscores (<code>_</code>), and dashes (<code>-</code>).</li>
 *   <li>First character: must be a lowercase letter (<code>a-z</code>) or underscore (<code>_</code>).</li>
 *   <li>Uppercase letters, spaces, and special characters (e.g., <code>@</code>, <code>.</code>, <code>:</code>, <code>?</code>) are not permitted.</li>
 * </ul>
 *
 * <p>The regex used is:
 * <pre>
 * ^[a-z_][a-z0-9_-]*$
 * </pre>
 *
 * <p>Explanation of the pattern:
 * <ul>
 *   <li><code>^</code> — start of the string.</li>
 *   <li><code>[a-z_]</code> — the first character must be a lowercase letter or underscore.</li>
 *   <li><code>[a-z0-9_-]*</code> — zero or more lowercase letters, digits, underscores, or dashes may follow.</li>
 *   <li><code>$</code> — end of the string.</li>
 * </ul>
 *
 * <p>Examples:
 * <ul>
 *   <li><code>my_resource</code> → valid</li>
 *   <li><code>resource-123</code> → valid</li>
 *   <li><code>_resource</code> → valid</li>
 *   <li><code>123resource</code> → invalid (starts with digit)</li>
 *   <li><code>MyResource</code> → invalid (uppercase letters not allowed)</li>
 *   <li><code>res@name</code> → invalid (special character not allowed)</li>
 * </ul>
 *
 * @param productName the resource name to validate
 * @return {@code true} if the name is valid according to Terraform conventions, {@code false} otherwise
 */
public boolean isValidTerraformResourceName(String productName) {
    if (productName == null) {
        return false;
    }
    return productName.matches("^[a-z_][a-z0-9_-]*$");
}
