package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.ItemRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.ItemResponseDTO;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Item;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.ItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ItemService {

    ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<ItemResponseDTO> getAllItems() {
        List<Item> items = itemRepository.findAll();
        List<ItemResponseDTO> response = items.stream().map((item -> new ItemResponseDTO(item))).toList();
        return response;
    }

    public ItemResponseDTO findById(Integer id) {
        Item item = itemRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        ItemResponseDTO response = new ItemResponseDTO(item);
        return response;
    }

    public ResponseEntity<ItemResponseDTO> createItem(ItemRequestDTO requestDTO) {
        Item item = toEntity(requestDTO);
        Item savedItem = itemRepository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ItemResponseDTO(savedItem));
    }

    public ItemResponseDTO updateItem(Integer itemId, ItemRequestDTO requestDTO) {
        Item item = itemRepository.findById(itemId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        item.setName(requestDTO.getName());
        item.setDescription(requestDTO.getDescription());
        item.setType(requestDTO.getType());
        itemRepository.save(item);
        return new ItemResponseDTO(item);
    }

    public void deleteItem(Integer itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        itemRepository.deleteById(itemId);
    }

    public Item getItemEntity(Integer id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Item toEntity(ItemRequestDTO requestDTO) {
        Item item = new Item();
        item.setName(requestDTO.getName());
        item.setDescription(requestDTO.getDescription());
        item.setType(requestDTO.getType());
        return item;
    }

    public ItemResponseDTO toDto(Item item) {
        return new ItemResponseDTO(item);
    }

}
