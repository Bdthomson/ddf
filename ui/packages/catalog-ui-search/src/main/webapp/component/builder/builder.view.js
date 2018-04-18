/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/


define([
    'backbone',
    'marionette',
    'underscore',
    'jquery',
    './builder.hbs',
    'js/CustomElements',
    'component/property/property.collection.view',
    'component/loading/loading.view',
    'component/available-types/available-types.view',
    'component/dropdown/dropdown.view',
    'component/singletons/metacard-definitions',
    'component/property/property.view',
    'component/property/property'
], function (Backbone, Marionette, _, $, template, CustomElements, PropertyCollectionView, LoadingView, AvailableTypesView, DropdownView, metacardDefinitions, PropertyView, Property) {

    let availableTypes;

    const ajaxCall = $.get({
        url: '/search/catalog/internal/builder/availabletypes'
    }).then((response) => {
        availableTypes = response;
    });

    return Marionette.LayoutView.extend({
            template,
            tagName: CustomElements.register('builder'),
            modelEvents: {
                'change:metacard': 'handleMetacard'
            },
            events: {
                'click .builder-edit': 'edit',
                'click .builder-save': 'save',
                'click .builder-cancel': 'cancel'
            },
            regions: {
                builderProperties: '> .builder-properties',
                builderAvailableType: '> .builder-select-available-type > .builder-select-available-type-dropdown'
            },
            initialize(options) {

                if (!availableTypes) {
                    const loadingview = new LoadingView();
                    ajaxCall.then(() => {
                        loadingview.remove();
                        this.model.set('availableTypes', availableTypes);
                        this.handleAvailableTypes();
                    });
                } else {
                    this.model.set('availableTypes', availableTypes);
                }

            },
            isSingleAvailableType() {
                const availableTypes = this.model.get('availableTypes');
                return availableTypes && availableTypes.availabletypes && availableTypes.availabletypes.length === 1;
            },
            isMultipleAvailableTypes() {
                const availableTypes = this.model.get('availableTypes');
                return availableTypes && availableTypes.availabletypes && availableTypes.availabletypes.length > 1;
            },
            showMetacardTypeSelection() {
                const enums = this.model.get('availableTypes').availabletypes.map((availableType) => ({ label: availableType.metacardType, value: availableType.metacardType }));

                const availableTypesModel = new Property({
                    label: "Select An Available Metacard Type",
                    value: [this.model.get('availableTypes').availabletypes[0].metacardType],
                    enum: enums,
                    id: "Select Metacard Type"
                });

                this.builderAvailableType.show(new PropertyView({
                    model: availableTypesModel
                }));

                this.builderAvailableType.currentView.turnOnEditing();

                this.listenTo(availableTypesModel, 'change:value', this.handleSelectedAvailableType);

                this.$el.addClass('is-selecting-available-types');
            },
            handleSystemTypes() {

                metacardDefinitions.metacardDefinitions.reduce((accumulator, currentValue) => {
                    console.log(currentValue);
                }, { availabletypes: [] })

                const mds = metacardDefinitions.metacardDefinitions;

                const allTypes = Object.keys(metacardDefinitions.metacardDefinitions)
                    .sort()
                    .reduce((accumulator, currentValue) => {
                        const visibleAttributes = Object.keys(mds[currentValue]);
                        accumulator.availabletypes.push({ metacardType: currentValue, visibleAttributes: visibleAttributes});
                        return accumulator;
                    }, { availabletypes: [] });

                this.model.set('availableTypes', allTypes);

                this.showMetacardTypeSelection();

            },
            handleAvailableTypes() {

                const availableTypes = this.model.get('availableTypes');

                if(this.isSingleAvailableType()) {
                    this.model.set('selectedAvailableType', this.model.get('availableTypes').availabletypes[0]);
                    this.showMetacardBuilder();
                } else if(this.isMultipleAvailableTypes()) {
                    this.showMetacardTypeSelection();
                } else {
                    this.handleSystemTypes();
                }
            },
            handleSelectedAvailableType() {
                this.$el.removeClass('is-selecting-available-types');

                const selectedAvailableType = this.builderAvailableType.currentView.model.getValue()[0];

                this.model.set('selectedAvailableType', selectedAvailableType);

                this.showMetacardBuilder();
            },
            showMetacardBuilder() {

                const selectedAvailableType = this.model.get('selectedAvailableType');

                const metacardDefinition = metacardDefinitions.metacardDefinitions[selectedAvailableType.metacardType];

                const propertyCollection = {
                    'metacard-type': selectedAvailableType.metacardType
                };

                selectedAvailableType.visibleAttributes
                    .filter((attribute) => !metacardDefinitions.isHiddenType(attribute))
                    .filter((attribute) => !metacardDefinition[attribute].readOnly)
                    .filter((attribute) => attribute !== "id")
                    .forEach((attribute) => {
                    if (metacardDefinition[attribute].multivalued) {
                      propertyCollection[attribute] = [];
                    } else if (metacardDefinitions.enums[attribute]) {
                      propertyCollection[attribute] = metacardDefinitions.enums[attribute][0];
                    } else {
                      propertyCollection[attribute] = "";
                    }
                });

                this.model.set('metacard', propertyCollection);
                this.$el.addClass('is-building');

            },
            handleMetacard() {
                const metacard = this.model.get('metacard');
                this.builderProperties.show(PropertyCollectionView.generatePropertyCollectionView([metacard]));
                this.builderProperties.currentView.$el.addClass("is-list");

            },
            onBeforeShow() {
                    this.handleAvailableTypes();
            },
            edit() {
                        this.$el.addClass('is-editing');
                        this.builderProperties.currentView.turnOnEditing();
                        this.builderProperties.currentView.focus();
            },
            cancel() {
                        this.$el.removeClass('is-editing');
                        this.builderProperties.currentView.revert();
                        this.builderProperties.currentView.turnOffEditing();
            },
            save() {
                        this.$el.removeClass('is-editing');

                        const editedMetacard = this.builderProperties.currentView.toPropertyJSON([], []);

                        const props = editedMetacard.properties;
                        editedMetacard.properties = Object.keys(editedMetacard.properties)
                            .filter((attributeName) => props[attributeName].length >= 1 && props[attributeName][0] !== "")
                            .reduce((accummulator, currentValue) => _.extend(accummulator, { [currentValue]: props[currentValue]}), {});

                        editedMetacard.properties['metacard-type'] = this.model.get('selectedAvailableType');

                        $.ajax({
                            type: 'POST',
                            url: '/services/catalog/?transform=input-propertyjson',
                            data: JSON.stringify(editedMetacard),
                            contentType: 'application/json'
                        }).then((response, status, xhr) => {
                            this.options.handleNewMetacard(xhr.getResponseHeader('id'));
                            this.options.close();
                        });

                        this.builderProperties.currentView.turnOffEditing();
            }
    });
});    