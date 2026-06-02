package com.garganttua.core.nativve.image.config.reflection;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.nativve.IReflectionConfiguration;
import com.garganttua.core.nativve.IReflectionConfigurationEntry;
import com.garganttua.core.reflection.IClass;

/**
 * In-memory model of a GraalVM {@code reflect-config.json} file: a mutable list of
 * {@link ReflectConfigEntry} entries that can be loaded from and saved to JSON.
 */
public class ReflectionConfiguration implements IReflectionConfiguration{
    private static final Logger log = Logger.getLogger(ReflectionConfiguration.class);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Loads a reflection configuration from a JSON file. If the file cannot be read,
	 * an empty configuration is returned rather than throwing.
	 *
	 * @param file the {@code reflect-config.json} file to read
	 * @return a configuration populated from {@code file}, or an empty one on read failure
	 */
	public static ReflectionConfiguration loadFromFile(File file) {
		log.trace("Entering loadFromFile with file: {}", file);
		List<IReflectionConfigurationEntry> entries;
		try {
			log.debug("Loading reflection configuration from file: {}", file);
			entries = objectMapper.readValue(file,
					objectMapper.getTypeFactory().constructCollectionType(List.class, ReflectConfigEntry.class));
			log.debug("Loaded {} reflection configuration entries from file", entries.size());
		} catch (IOException e) {
			log.warn("Failed to load reflection configuration from file, initializing empty list: {}", file);
			entries = new ArrayList<>();
		}
		ReflectionConfiguration config = new ReflectionConfiguration();
		config.setEntries(entries);
		log.trace("Exiting loadFromFile");
		return config;
	}

	private List<IReflectionConfigurationEntry> entries;

	/**
	 * Returns the live list of reflection entries.
	 *
	 * @return the configured entries
	 */
	public List<IReflectionConfigurationEntry> getEntries() {
		return entries;
	}

	@Override
	public void setEntries(List<IReflectionConfigurationEntry> entries) {
		log.trace("Setting entries with {} items", entries.size());
		this.entries = entries;
	}

	@Override
	public void addEntry(IReflectionConfigurationEntry entry) {
		log.trace("Entering addEntry for: {}", entry.getName());
		removeEntry(entry);
		entries.add(entry);
		log.debug("Added reflection configuration entry: {}", entry.getName());
		log.trace("Exiting addEntry");
	}

	@Override
	public void removeEntry(IReflectionConfigurationEntry entry) {
		log.trace("Entering removeEntry for: {}", entry.getName());
		entries = entries.stream().filter(e -> !e.getName().equals(entry.getName())).collect(Collectors.toList());
		log.debug("Removed reflection configuration entry: {}", entry.getName());
		log.trace("Exiting removeEntry");
	}

	@Override
	public void updateEntry(IReflectionConfigurationEntry updatedEntry) {
		log.trace("Entering updateEntry for: {}", updatedEntry.getName());
		for (int i = 0; i < entries.size(); i++) {
			IReflectionConfigurationEntry entry = entries.get(i);
			if (entry.getName().equals(updatedEntry.getName())) {
				entries.set(i, updatedEntry);
				log.debug("Updated reflection configuration entry: {}", updatedEntry.getName());
				break;
			}
		}
		log.trace("Exiting updateEntry");
	}

	@Override
	public void saveToFile(File file) throws IOException {
		log.trace("Entering saveToFile with file: {}", file);
		log.debug("Saving {} reflection configuration entries to file: {}", entries.size(), file);
		objectMapper.writeValue(file, this.entries);
		log.debug("Reflection configuration saved successfully to file: {}", file);
		log.trace("Exiting saveToFile");
	}

	@Override
	public Optional<IReflectionConfigurationEntry> findEntryByType(IClass<?> type) {
		log.trace("Entering findEntryByType for type: {}", type.getName());
		Optional<IReflectionConfigurationEntry> result = entries.stream().filter(entry -> entry.getName().equals(type.getName())).findFirst();
		log.debug("Found entry for type {}: {}", type.getName(), result.isPresent());
		log.trace("Exiting findEntryByType");
		return result;
	}
}
