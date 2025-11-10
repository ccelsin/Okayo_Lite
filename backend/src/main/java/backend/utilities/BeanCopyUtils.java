package backend.utilities;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Classe utilitaire pour copier des propriétés d’un objet source vers un objet cible
 * tout en ignorant les valeurs {@code null}.
 *
 * <p>Elle repose sur la classe {@link BeanUtils} de Spring et permet de mettre à jour
 * un objet existant sans écraser ses propriétés non renseignées.</p>
 *
 * <p>Exemple d’utilisation :</p>
 * <pre>{@code
 * UserDto dto = new UserDto("John", null, "Paris");
 * User entity = new User("Jane", "Doe", "Lyon");
 *
 * BeanCopyUtils.copyNonNullProperties(dto, entity);
 * // Résultat : entity = ("John", "Doe", "Paris")
 * }</pre>
 *
 * <p>Cette approche est très utile pour les opérations de mise à jour
 * (PATCH/PUT) dans les services.</p>
 */
public final class BeanCopyUtils {

    /**
     * Constructeur privé pour empêcher l’instanciation.
     * <p>Cette classe ne contient que des méthodes statiques.</p>
     */
    private BeanCopyUtils() {}

    /**
     * Copie les propriétés non nulles de l’objet source vers l’objet cible.
     *
     * <p>Les propriétés listées dans {@code extraIgnored} seront également ignorées
     * pendant la copie, même si elles ne sont pas nulles.</p>
     *
     * @param source        l’objet source contenant les nouvelles valeurs
     * @param target        l’objet cible à mettre à jour
     * @param extraIgnored  noms de propriétés à ignorer explicitement
     */
    public static void copyNonNullProperties(Object source, Object target, String... extraIgnored) {
        String[] nullProps = getNullOrIgnoredPropertyNames(source, extraIgnored);
        // Copie toutes les propriétés sauf celles nulles ou ignorées explicitement
        BeanUtils.copyProperties(source, target, nullProps);
    }

    /**
     * Récupère les noms des propriétés à ignorer (nulles ou explicitement listées).
     *
     * <p>Cette méthode inspecte les propriétés de l’objet source à l’aide d’un
     * {@link BeanWrapper} et ajoute à la liste d’ignorés toutes les propriétés
     * dont la valeur est {@code null}.</p>
     *
     * @param source        l’objet à analyser
     * @param extraIgnored  noms de propriétés à toujours ignorer (ex. "id")
     * @return un tableau de noms de propriétés à ne pas copier
     */
    private static String[] getNullOrIgnoredPropertyNames(Object source, String... extraIgnored) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        Set<String> ignored = new HashSet<>(Arrays.asList(extraIgnored));
        ignored.add("class"); // toujours ignorer la propriété "class"

        for (PropertyDescriptor pd : src.getPropertyDescriptors()) {
            String name = pd.getName();
            if ("class".equals(name)) continue;
            Object value = src.getPropertyValue(name);
            if (value == null) {
                ignored.add(name);
            }
        }
        return ignored.toArray(new String[0]);
    }
}
