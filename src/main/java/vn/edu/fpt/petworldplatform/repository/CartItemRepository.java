package vn.edu.fpt.petworldplatform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.petworldplatform.entity.CartItem;
import vn.edu.fpt.petworldplatform.entity.Pets;
import vn.edu.fpt.petworldplatform.entity.Product;

import java.util.Optional;
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

    // Tìm bằng đối tượng Pets
    Optional<CartItem> findByCart_CartIdAndPet(Integer cartId, Pets pet);

    // Tìm bằng đối tượng Product
    Optional<CartItem> findByCart_CartIdAndProduct(Integer cartId, Product product);
}
